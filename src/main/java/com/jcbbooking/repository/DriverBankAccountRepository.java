package com.jcbbooking.repository;

import com.jcbbooking.model.DriverBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DriverBankAccountRepository extends JpaRepository<DriverBankAccount, Long> {
    Optional<DriverBankAccount> findByDriverId(Long driverId);
    Optional<DriverBankAccount> findByUserId(Long userId);
}
