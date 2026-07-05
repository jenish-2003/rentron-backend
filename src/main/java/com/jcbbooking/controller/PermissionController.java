package com.jcbbooking.controller;

import com.jcbbooking.model.Permission;
import com.jcbbooking.repository.PermissionRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        log.info("REST request to fetch all permissions");
        List<Permission> list = permissionRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Permission>> savePermission(@RequestBody Permission permission) {
        log.info("REST request to save/update permission: {}", permission);
        
        // Uniqueness validation on code
        if (permission.getId() == null || permission.getId() == 0) {
            permission.setId(null);
            if (permissionRepository.findByPermissionCode(permission.getPermissionCode()).isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Permission code already exists"));
            }
        } else {
            Permission existing = permissionRepository.findById(permission.getId()).orElse(null);
            if (existing != null && !existing.getPermissionCode().equals(permission.getPermissionCode())) {
                if (permissionRepository.findByPermissionCode(permission.getPermissionCode()).isPresent()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Permission code already exists"));
                }
            }
        }
        
        Permission saved = permissionRepository.save(permission);
        String msg = (permission.getId() == null) ? "Permission created successfully" : "Permission updated successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        log.info("REST request to delete permission ID: {}", id);
        if (!permissionRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Permission not found"));
        }
        permissionRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted successfully"));
    }
}
