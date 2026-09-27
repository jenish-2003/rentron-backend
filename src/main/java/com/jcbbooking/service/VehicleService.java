package com.jcbbooking.service;

import com.jcbbooking.dto.VehicleRegistrationRequest;
import com.jcbbooking.dto.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse registerVehicle(VehicleRegistrationRequest request, Long authenticatedUserId);
    List<VehicleResponse> getMyVehicles(Long authenticatedUserId);
    List<VehicleResponse> getVehiclesByDriverId(Long driverId);
    List<VehicleResponse> getAllVehiclesForAdmin();
    VehicleResponse getVehicleById(Long id);
    VehicleResponse approveVehicle(Long id);
    VehicleResponse rejectVehicle(Long id, String reason);
}
