package com.jcbbooking.repository;

import com.jcbbooking.model.RentalPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalPackageRepository extends JpaRepository<RentalPackage, Long> {
    List<RentalPackage> findAllByActiveTrue();
    List<RentalPackage> findAllByActiveTrueAndVehicleCategory(String vehicleCategory);
    List<RentalPackage> findAllByVehicleCategory(String vehicleCategory);
    Optional<RentalPackage> findByNameIgnoreCase(String name);
}
