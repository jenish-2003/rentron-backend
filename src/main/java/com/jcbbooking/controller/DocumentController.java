package com.jcbbooking.controller;

import com.jcbbooking.model.Document;
import com.jcbbooking.repository.DocumentRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentRepository documentRepository;

    @Value("${core.fileTransfer.primaryUploadFolder}")
    private String primaryUploadFolder;

    private String resolveBaseDirectory() {
        String baseDirStr = primaryUploadFolder;
        try {
            File baseDir = new File(baseDirStr);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            if (!baseDir.canWrite()) {
                baseDirStr = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "images";
                new File(baseDirStr).mkdirs();
            }
        } catch (Exception ex) {
            log.warn("Lacking permissions for configured path: {}. Falling back to workspace folders.", baseDirStr);
            baseDirStr = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "images";
            new File(baseDirStr).mkdirs();
        }
        return baseDirStr;
    }

    @PostMapping("/upload")
    @Transactional
    public ResponseEntity<ApiResponse<Document>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType, // DRIVER, CONTRACTOR
            @RequestParam("entityId") Long entityId,
            @RequestParam("documentType") String documentType) {

        log.info("REST request to upload document immediately: type={}, entityType={}, entityId={}", 
                documentType, entityType, entityId);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
        }

        try {
            String baseDirStr = resolveBaseDirectory();

            // Create target folder path: base + /driver/0/ or base + /contractor/0/
            String targetFolderStr = baseDirStr + File.separator + entityType.toLowerCase() + File.separator + entityId;
            File targetFolder = new File(targetFolderStr);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }

            // Target file name & destination path
            String originalFileName = file.getOriginalFilename();
            String cleanFileName = System.currentTimeMillis() + "_" + (originalFileName != null ? originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_") : "file");
            Path destinationPath = Paths.get(targetFolderStr, cleanFileName);

            // Copy file to disk
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File successfully written to: {}", destinationPath);

            // Create document metadata
            Document document = Document.builder()
                    .entityType(entityType.toUpperCase())
                    .entityId(entityId)
                    .documentType(documentType)
                    .fileName(cleanFileName)
                    .fileType(file.getContentType())
                    .filePath(destinationPath.toAbsolutePath().toString())
                    .build();

            Document saved = documentRepository.save(document);
            return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", saved));

        } catch (IOException e) {
            log.error("Failed to store file", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to store file: " + e.getMessage()));
        }
    }

    @PostMapping("/associate")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> associateDocuments(
            @RequestParam("documentIds") List<Long> documentIds,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId) {
        
        log.info("REST request to associate documents {} with entity {} (ID: {})", 
                documentIds, entityType, entityId);

        String baseDirStr = resolveBaseDirectory();

        // Target permanent folder
        String targetFolderStr = baseDirStr + File.separator + entityType.toLowerCase() + File.separator + entityId;
        File targetFolder = new File(targetFolderStr);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        for (Long id : documentIds) {
            documentRepository.findById(id).ifPresent(doc -> {
                File oldFile = new File(doc.getFilePath());
                File newFile = new File(targetFolder, doc.getFileName());
                
                if (oldFile.exists()) {
                    try {
                        Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        doc.setFilePath(newFile.getAbsolutePath());
                        log.info("Successfully moved file from temp directory to: {}", newFile.getAbsolutePath());
                    } catch (IOException e) {
                        log.error("Failed to move file to permanent folder for ID: {}", id, e);
                    }
                }
                
                doc.setEntityType(entityType.toUpperCase());
                doc.setEntityId(entityId);
                documentRepository.save(doc);
            });
        }

        return ResponseEntity.ok(ApiResponse.success("Documents associated successfully"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        log.info("REST request to delete document ID: {}", id);
        documentRepository.findById(id).ifPresent(doc -> {
            try {
                File file = new File(doc.getFilePath());
                if (file.exists()) {
                    file.delete();
                    log.info("File successfully deleted from disk: {}", doc.getFilePath());
                }
            } catch (Exception ex) {
                log.error("Failed to delete file from disk", ex);
            }
            documentRepository.delete(doc);
        });
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<Document>>> getDocumentsForEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        log.info("REST request to fetch documents for entity: {} (ID: {})", entityType, entityId);
        List<Document> docs = documentRepository.findAllByEntityTypeAndEntityId(entityType.toUpperCase(), entityId);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved successfully", docs));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        log.info("REST request to download document ID: {}", id);
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(doc.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(doc.getFileType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }
}
