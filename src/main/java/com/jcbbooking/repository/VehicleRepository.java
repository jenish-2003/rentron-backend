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
}
