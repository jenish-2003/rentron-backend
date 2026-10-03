package com.jcbbooking.repository;

import com.jcbbooking.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Vehicle> findByDriverIdOrderByCreatedAtDesc(Long driverId);

    List<Vehicle> findByStatusOrderByCreatedAtDesc(String status);

    List<Vehicle> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Vehicle v WHERE " +
           "(:userId IS NOT NULL AND v.userId = :userId) OR " +
           "(:driverId IS NOT NULL AND v.driverId = :driverId)")
    List<Vehicle> findByUserIdOrDriverId(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("driverId") Long driverId);
}
