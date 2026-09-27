package com.jcbbooking.repository;

import com.jcbbooking.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

    List<VehicleModel> findByVehicleTypeIdAndActiveTrueOrderByDisplayOrderAsc(Long vehicleTypeId);

    List<VehicleModel> findByVehicleTypeIdOrderByDisplayOrderAsc(Long vehicleTypeId);

    List<VehicleModel> findByActiveTrueOrderByDisplayOrderAsc();

    List<VehicleModel> findAllByOrderByDisplayOrderAsc();

    Optional<VehicleModel> findByVehicleTypeIdAndCodeIgnoreCase(Long vehicleTypeId, String code);

    boolean existsByVehicleTypeIdAndNameIgnoreCase(Long vehicleTypeId, String name);

    boolean existsByVehicleTypeIdAndNameIgnoreCaseAndIdNot(Long vehicleTypeId, String name, Long id);
}
