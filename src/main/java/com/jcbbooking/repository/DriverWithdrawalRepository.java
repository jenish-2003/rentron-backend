package com.jcbbooking.repository;

import com.jcbbooking.model.DriverWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DriverWithdrawalRepository extends JpaRepository<DriverWithdrawal, Long> {
    List<DriverWithdrawal> findAllByDriverIdOrderByRequestedAtDesc(Long driverId);
    List<DriverWithdrawal> findAllByUserIdOrderByRequestedAtDesc(Long userId);
}
