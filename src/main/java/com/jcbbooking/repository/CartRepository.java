package com.jcbbooking.repository;

import com.jcbbooking.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findAllByCustomerId(Long customerId);
    Optional<Cart> findByIdAndCustomerId(Long id, Long customerId);
    void deleteAllByCustomerId(Long customerId);
}
