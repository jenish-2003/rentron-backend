package com.jcbbooking.repository;

import com.jcbbooking.model.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractorRepository extends JpaRepository<Contractor, Long> {
    Optional<Contractor> findByPhone(String phone);
    Optional<Contractor> findByUserId(Long userId);
    List<Contractor> findAllByStatus(String status);
    boolean existsByPhone(String phone);
}
