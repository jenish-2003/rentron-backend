package com.jcbbooking.controller;

import com.jcbbooking.model.Product;
import com.jcbbooking.repository.ProductRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        log.info("REST request to get all products");
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", productRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProductById(@PathVariable Long id) {
        log.info("REST request to get product by id: {}", id);
        return productRepository.findById(id)
                .map(product -> ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Product>> saveProduct(@RequestBody Product product) {
        log.info("REST request to save product: {}", product);
        boolean isNew = (product.getId() == null || product.getId() == 0);

        if (isNew) {
            product.setId(null);
            if (productRepository.existsByCode(product.getCode())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Product code already exists: " + product.getCode()));
            }
        } else {
            Product existing = productRepository.findById(product.getId()).orElse(null);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }
            existing.setName(product.getName());
            existing.setCode(product.getCode());
            existing.setProductType(product.getProductType());
            existing.setCategory(product.getCategory());
            existing.setDescription(product.getDescription());
            if (product.getActive() != null) {
                existing.setActive(product.getActive());
            }
            product = existing;
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(ApiResponse.success(isNew ? "Product created successfully" : "Product updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("REST request to delete product ID: {}", id);
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }
}
