package com.jcbbooking.repository;

import com.jcbbooking.model.DriverBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverBankAccountRepository extends JpaRepository<DriverBankAccount, Long> {
    Optional<DriverBankAccount> findByDriverId(Long driverId);
    Optional<DriverBankAccount> findByUserId(Long userId);
    List<DriverBankAccount> findAllByDriverIdOrderByIdDesc(Long driverId);
    List<DriverBankAccount> findAllByUserIdOrderByIdDesc(Long userId);
    Optional<DriverBankAccount> findByIdAndUserId(Long id, Long userId);
}
