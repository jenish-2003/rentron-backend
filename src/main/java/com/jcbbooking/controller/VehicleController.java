package com.jcbbooking.controller;

import com.jcbbooking.model.Vehicle;
import com.jcbbooking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllVehicles() {
        List<Vehicle> list = vehicleService.getAllVehicles();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getVehiclesByUserId(@PathVariable Long userId) {
        List<Vehicle> list = vehicleService.getVehiclesByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<Map<String, Object>> getVehiclesByDriverId(@PathVariable Long driverId) {
        List<Vehicle> list = vehicleService.getVehiclesByDriverId(driverId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVehicleById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + id));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", vehicle);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createVehicle(@RequestBody Vehicle vehicle) {
        Vehicle saved = vehicleService.createVehicle(vehicle);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle created successfully");
        response.put("data", saved);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateVehicle(@PathVariable Long id, @RequestBody Vehicle details) {
        Vehicle updated = vehicleService.updateVehicle(id, details);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle updated successfully");
        response.put("data", updated);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/approve", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> approveVehicle(@PathVariable Long id) {
        Vehicle approved = vehicleService.approveVehicle(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle approved successfully");
        response.put("data", approved);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> rejectVehicle(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String reasonParam) {
        String reason = "Documents incomplete or invalid";
        if (body != null && body.containsKey("reason") && body.get("reason") != null && !body.get("reason").trim().isEmpty()) {
            reason = body.get("reason").trim();
        } else if (reasonParam != null && !reasonParam.trim().isEmpty()) {
            reason = reasonParam.trim();
        }
        Vehicle rejected = vehicleService.rejectVehicle(id, reason);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle rejected successfully");
        response.put("data", rejected);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/activate", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> activateVehicle(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        Vehicle activated = vehicleService.activateVehicle(id, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Vehicle activated successfully");
        response.put("data", activated);
        return ResponseEntity.ok(response);
    }
}
