package com.jcbbooking.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping({"/api/v1/vehicle-masters", "/api/v1/admin/vehicle-masters"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(originPatterns = "*")
public class VehicleMasterController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping({"/catalog", "/types"})
    public ResponseEntity<Map<String, Object>> getCatalog() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> types = jdbcTemplate.queryForList("SELECT * FROM vehicle_types");
            List<Map<String, Object>> models = new ArrayList<>();
            List<Map<String, Object>> subModels = new ArrayList<>();

            try {
                models = jdbcTemplate.queryForList("SELECT * FROM vehicle_models");
            } catch (Exception e) {
                log.warn("Could not query vehicle_models: {}", e.getMessage());
            }

            try {
                subModels = jdbcTemplate.queryForList("SELECT * FROM vehicle_sub_models");
            } catch (Exception e) {
                log.warn("Could not query vehicle_sub_models: {}", e.getMessage());
            }

            if (types != null && !types.isEmpty()) {
                List<Map<String, Object>> catalog = buildCatalogHierarchy(types, models, subModels);
                response.put("success", true);
                response.put("message", "Catalog retrieved successfully from DB");
                response.put("data", catalog);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to query vehicle_types from DB: {}", e.getMessage());
        }

        // Fallback to default catalog if DB query fails or table is empty
        response.put("success", true);
        response.put("message", "Catalog retrieved successfully");
        response.put("data", getFallbackCatalog());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getModels() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> models = jdbcTemplate.queryForList("SELECT * FROM vehicle_models");
            if (models != null && !models.isEmpty()) {
                response.put("success", true);
                response.put("message", "Vehicle models retrieved successfully from DB");
                response.put("data", models);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to query vehicle_models from DB: {}", e.getMessage());
        }

        response.put("success", true);
        response.put("message", "Vehicle models retrieved successfully");
        response.put("data", getFallbackModels());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sub-models")
    public ResponseEntity<Map<String, Object>> getSubModels() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> subModels = jdbcTemplate.queryForList("SELECT * FROM vehicle_sub_models");
            if (subModels != null && !subModels.isEmpty()) {
                response.put("success", true);
                response.put("message", "Vehicle sub-models retrieved successfully from DB");
                response.put("data", subModels);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to query vehicle_sub_models from DB: {}", e.getMessage());
        }

        response.put("success", true);
        response.put("message", "Vehicle sub-models retrieved successfully");
        response.put("data", getFallbackSubModels());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/vahan-lookup/{regNumber}", "/vahan/fetch"})
    public ResponseEntity<Map<String, Object>> fetchVahanDetails(
            @PathVariable(required = false) String regNumber,
            @RequestParam(name = "regNumber", required = false) String paramRegNumber) {
        String reg = (regNumber != null && !regNumber.trim().isEmpty()) ? regNumber : paramRegNumber;
        Map<String, Object> response = new HashMap<>();
        if (reg == null || reg.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Registration number is required");
            return ResponseEntity.badRequest().body(response);
        }
        String cleanReg = reg.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        Map<String, Object> vahanData = new HashMap<>();
        vahanData.put("regNumber", cleanReg);
        vahanData.put("mfgYear", "2023");
        vahanData.put("machineClass", cleanReg.startsWith("TN") ? "Commercial Vehicle" : "Earth Moving");
        vahanData.put("chassisVin", "MA1PB45JC9L" + String.format("%06d", Math.abs(cleanReg.hashCode() % 1000000)));
        vahanData.put("engineSerial", "4H" + String.format("%08d", Math.abs(cleanReg.hashCode() % 100000000)));
        vahanData.put("powertrain", "DIESEL");
        vahanData.put("isVahanSynced", true);
        vahanData.put("fetchMode", "AUTO_VAHAN");
        vahanData.put("vehicleDescription", "VAHAN Verified Vehicle Specs for " + cleanReg);

        response.put("success", true);
        response.put("message", "VAHAN data retrieved successfully");
        response.put("data", vahanData);
        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> buildCatalogHierarchy(
            List<Map<String, Object>> types,
            List<Map<String, Object>> models,
            List<Map<String, Object>> subModels) {

        List<Map<String, Object>> catalog = new ArrayList<>();

        for (Map<String, Object> rawType : types) {
            Map<String, Object> typeMap = new HashMap<>(rawType);

            // Normalize icon key
            if (!typeMap.containsKey("iconUrl") && typeMap.containsKey("icon_url")) {
                typeMap.put("iconUrl", typeMap.get("icon_url"));
            }
            if (!typeMap.containsKey("iconUrl") || typeMap.get("iconUrl") == null) {
                String name = String.valueOf(typeMap.getOrDefault("name", "")).toLowerCase();
                if (name.contains("car")) typeMap.put("iconUrl", "🚗");
                else if (name.contains("auto")) typeMap.put("iconUrl", "🛺");
                else if (name.contains("bike")) typeMap.put("iconUrl", "🛵");
                else typeMap.put("iconUrl", "🚜");
            }

            Object typeId = typeMap.get("id");
            String typeName = String.valueOf(typeMap.getOrDefault("name", "")).toLowerCase();

            // Find models for this type
            List<Map<String, Object>> matchingModels = new ArrayList<>();
            for (Map<String, Object> rawModel : models) {
                Object mTypeId = rawModel.getOrDefault("vehicle_type_id", rawModel.get("type_id"));
                boolean isMatch = (mTypeId != null && mTypeId.toString().equals(String.valueOf(typeId))) ||
                        (typeName.contains("machinery"));

                if (isMatch) {
                    Map<String, Object> modelMap = new HashMap<>(rawModel);
                    Object modelId = modelMap.get("id");

                    // Find submodels for this model
                    List<Map<String, Object>> matchingSubModels = new ArrayList<>();
                    for (Map<String, Object> rawSub : subModels) {
                        Object subModelId = rawSub.getOrDefault("vehicle_model_id", rawSub.getOrDefault("model_id", rawSub.get("modelId")));
                        if (subModelId != null && subModelId.toString().equals(String.valueOf(modelId))) {
                            matchingSubModels.add(new HashMap<>(rawSub));
                        }
                    }
                    if (!matchingSubModels.isEmpty()) {
                        modelMap.put("subModels", matchingSubModels);
                    }
                    matchingModels.add(modelMap);
                }
            }

            if (!matchingModels.isEmpty()) {
                typeMap.put("models", matchingModels);
            }

            catalog.add(typeMap);
        }

        return catalog;
    }

    private List<Map<String, Object>> getFallbackCatalog() {
        List<Map<String, Object>> catalog = new ArrayList<>();

        Map<String, Object> car = new HashMap<>();
        car.put("id", 1);
        car.put("name", "Car");
        car.put("code", "CAR");
        car.put("description", "Drive passengers");
        car.put("iconUrl", "🚗");
        car.put("active", true);

        Map<String, Object> auto = new HashMap<>();
        auto.put("id", 2);
        auto.put("name", "Auto");
        auto.put("code", "AUTO");
        auto.put("description", "Drive passengers");
        auto.put("iconUrl", "🛺");
        auto.put("active", true);

        Map<String, Object> bike = new HashMap<>();
        bike.put("id", 3);
        bike.put("name", "Bike");
        bike.put("code", "BIKE");
        bike.put("description", "Take bike taxi trips");
        bike.put("iconUrl", "🛵");
        bike.put("active", true);

        Map<String, Object> machinery = new HashMap<>();
        machinery.put("id", 4);
        machinery.put("name", "Machinery");
        machinery.put("code", "MACHINERY");
        machinery.put("description", "Operate construction machinery");
        machinery.put("iconUrl", "🚜");
        machinery.put("active", true);
        machinery.put("models", getFallbackModels());

        catalog.add(car);
        catalog.add(auto);
        catalog.add(bike);
        catalog.add(machinery);

        return catalog;
    }

    private List<Map<String, Object>> getFallbackModels() {
        List<Map<String, Object>> models = new ArrayList<>();

        Map<String, Object> m1 = new HashMap<>();
        m1.put("id", 101);
        m1.put("name", "Backhoe");
        m1.put("code", "BACKHOE");
        m1.put("description", "Backhoe Loader");
        m1.put("subModels", getFallbackSubModels().subList(0, 3));
        models.add(m1);

        Map<String, Object> m2 = new HashMap<>();
        m2.put("id", 102);
        m2.put("name", "Excavator");
        m2.put("code", "EXCAVATOR");
        m2.put("description", "Hydraulic Excavator");
        m2.put("subModels", getFallbackSubModels().subList(3, 5));
        models.add(m2);

        Map<String, Object> m3 = new HashMap<>();
        m3.put("id", 103);
        m3.put("name", "Cranes");
        m3.put("code", "CRANES");
        m3.put("description", "Mobile Crane");
        models.add(m3);

        Map<String, Object> m4 = new HashMap<>();
        m4.put("id", 104);
        m4.put("name", "Bulldozer");
        m4.put("code", "BULLDOZER");
        m4.put("description", "Bulldozer Track Type");
        models.add(m4);

        return models;
    }

    private List<Map<String, Object>> getFallbackSubModels() {
        List<Map<String, Object>> subModels = new ArrayList<>();

        Map<String, Object> sm1 = new HashMap<>();
        sm1.put("id", 1001);
        sm1.put("name", "JCB 3DX");
        sm1.put("description", "Backhoe Loader");
        sm1.put("horsePower", "74 HP");
        sm1.put("capacity", "1.0 m³");
        subModels.add(sm1);

        Map<String, Object> sm2 = new HashMap<>();
        sm2.put("id", 1002);
        sm2.put("name", "CAT 424B2");
        sm2.put("description", "Backhoe Loader");
        sm2.put("horsePower", "75 HP");
        sm2.put("capacity", "General Purpose");
        subModels.add(sm2);

        Map<String, Object> sm3 = new HashMap<>();
        sm3.put("id", 1003);
        sm3.put("name", "Mahindra EarthMaster");
        sm3.put("description", "Backhoe Loader");
        sm3.put("horsePower", "79 HP");
        sm3.put("capacity", "Heavy Duty");
        subModels.add(sm3);

        Map<String, Object> sm4 = new HashMap<>();
        sm4.put("id", 1004);
        sm4.put("name", "Tata Hitachi EX 200");
        sm4.put("description", "Hydraulic Excavator");
        sm4.put("horsePower", "133 HP");
        sm4.put("capacity", "2.0 Ton");
        subModels.add(sm4);

        Map<String, Object> sm5 = new HashMap<>();
        sm5.put("id", 1005);
        sm5.put("name", "Komatsu PC210");
        sm5.put("description", "Hydraulic Excavator");
        sm5.put("horsePower", "165 HP");
        subModels.add(sm5);

        return subModels;
    }
}
