package com.jcbbooking.repository;

import com.jcbbooking.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);
    List<Product> findAllByActive(Boolean active);
    List<Product> findAllByProductType(String productType);
    boolean existsByCode(String code);
}
