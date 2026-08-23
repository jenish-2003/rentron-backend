package com.jcbbooking.repository;

import com.jcbbooking.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByPhone(String phone);
    Optional<Driver> findByUserId(Long userId);
    List<Driver> findAllByContractorId(Long contractorId);
    List<Driver> findAllByStatus(String status);
    boolean existsByPhone(String phone);
    boolean existsByLicenseNumber(String licenseNumber);
}
