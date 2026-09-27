package com.jcbbooking.repository;

import com.jcbbooking.model.VehicleSubModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleSubModelRepository extends JpaRepository<VehicleSubModel, Long> {

    List<VehicleSubModel> findByVehicleModelIdAndActiveTrueOrderByDisplayOrderAsc(Long vehicleModelId);

    List<VehicleSubModel> findByVehicleModelIdOrderByDisplayOrderAsc(Long vehicleModelId);

    List<VehicleSubModel> findByActiveTrueOrderByDisplayOrderAsc();

    List<VehicleSubModel> findAllByOrderByDisplayOrderAsc();

    Optional<VehicleSubModel> findByVehicleModelIdAndCodeIgnoreCase(Long vehicleModelId, String code);

    boolean existsByVehicleModelIdAndNameIgnoreCase(Long vehicleModelId, String name);

    boolean existsByVehicleModelIdAndNameIgnoreCaseAndIdNot(Long vehicleModelId, String name, Long id);
}
