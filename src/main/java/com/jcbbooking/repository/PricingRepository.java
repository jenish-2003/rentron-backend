package com.jcbbooking.repository;

import com.jcbbooking.model.Pricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRepository extends JpaRepository<Pricing, Long> {
    Optional<Pricing> findByProductIdAndActiveTrue(Long productId);
    Optional<Pricing> findByProductId(Long productId);
    List<Pricing> findAllByActive(Boolean active);
}
