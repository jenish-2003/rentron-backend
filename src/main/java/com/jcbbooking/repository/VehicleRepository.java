package com.jcbbooking.repository;

import com.jcbbooking.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByDriverId(Long driverId);

    List<Vehicle> findByUserId(Long userId);

    List<Vehicle> findByDriverIdAndIsActiveTrueAndStatus(Long driverId, String status);

    List<Vehicle> findByUserIdAndIsActiveTrueAndStatus(Long userId, String status);

    Optional<Vehicle> findByRegNumberIgnoreCase(String regNumber);

    boolean existsByRegNumberIgnoreCase(String regNumber);

    boolean existsByRegNumberIgnoreCaseAndIdNot(String regNumber, Long id);

    @Query("SELECT v FROM Vehicle v WHERE (v.driverId = :driverId OR v.userId = :userId) " +
           "AND v.status = 'APPROVED' AND v.isActive = true " +
           "AND (:typeId IS NULL OR v.vehicleTypeId = :typeId) " +
           "AND (:modelId IS NULL OR v.vehicleModelId = :modelId) " +
           "AND (:subModelId IS NULL OR v.vehicleSubModelId = :subModelId)")
    List<Vehicle> findMatchingApprovedActiveVehicles(@Param("driverId") Long driverId,
                                                      @Param("userId") Long userId,
                                                      @Param("typeId") Long typeId,
                                                      @Param("modelId") Long modelId,
                                                      @Param("subModelId") Long subModelId);

    @Query("SELECT COUNT(v) > 0 FROM Vehicle v WHERE (v.driverId = :driverId OR v.userId = :userId) " +
           "AND v.status = 'APPROVED' AND v.isActive = true " +
           "AND (:typeId IS NULL OR v.vehicleTypeId = :typeId) " +
           "AND (:modelId IS NULL OR v.vehicleModelId = :modelId) " +
           "AND (:subModelId IS NULL OR v.vehicleSubModelId = :subModelId)")
    boolean hasMatchingApprovedActiveVehicle(@Param("driverId") Long driverId,
                                             @Param("userId") Long userId,
                                             @Param("typeId") Long typeId,
                                             @Param("modelId") Long modelId,
                                             @Param("subModelId") Long subModelId);
}
