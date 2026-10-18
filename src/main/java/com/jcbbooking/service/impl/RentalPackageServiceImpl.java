package com.jcbbooking.service.impl;

import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.RentalPackage;
import com.jcbbooking.repository.RentalPackageRepository;
import com.jcbbooking.service.RentalPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalPackageServiceImpl implements RentalPackageService {

    private final RentalPackageRepository rentalPackageRepository;

    @Override
    public List<RentalPackage> getAllPackages() {
        return rentalPackageRepository.findAll();
    }

    @Override
    public List<RentalPackage> getActivePackages(String vehicleCategory) {
        if (vehicleCategory != null && !vehicleCategory.trim().isEmpty() && !"ALL".equalsIgnoreCase(vehicleCategory)) {
            List<RentalPackage> filtered = rentalPackageRepository.findAllByActiveTrueAndVehicleCategory(vehicleCategory.toUpperCase());
            if (!filtered.isEmpty()) {
                return filtered;
            }
        }
        return rentalPackageRepository.findAllByActiveTrue();
    }

    @Override
    public RentalPackage getPackageById(Long id) {
        return rentalPackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RentalPackage not found with id: " + id));
    }

    @Override
    @Transactional
    public RentalPackage createPackage(RentalPackage rentalPackage) {
        log.info("Creating rental package: {}", rentalPackage.getName());
        return rentalPackageRepository.save(rentalPackage);
    }

    @Override
    @Transactional
    public RentalPackage updatePackage(Long id, RentalPackage updateReq) {
        log.info("Updating rental package ID: {}", id);
        RentalPackage existing = getPackageById(id);

        if (updateReq.getName() != null) existing.setName(updateReq.getName());
        if (updateReq.getVehicleCategory() != null) existing.setVehicleCategory(updateReq.getVehicleCategory());
        if (updateReq.getIncludedHours() != null) existing.setIncludedHours(updateReq.getIncludedHours());
        if (updateReq.getIncludedKm() != null) existing.setIncludedKm(updateReq.getIncludedKm());
        if (updateReq.getBasePackageFare() != null) existing.setBasePackageFare(updateReq.getBasePackageFare());
        if (updateReq.getExtraKmPrice() != null) existing.setExtraKmPrice(updateReq.getExtraKmPrice());
        if (updateReq.getExtraHourPrice() != null) existing.setExtraHourPrice(updateReq.getExtraHourPrice());
        if (updateReq.getBookingFee() != null) existing.setBookingFee(updateReq.getBookingFee());
        if (updateReq.getTaxPercentage() != null) existing.setTaxPercentage(updateReq.getTaxPercentage());
        if (updateReq.getActive() != null) existing.setActive(updateReq.getActive());

        return rentalPackageRepository.save(existing);
    }

    @Override
    @Transactional
    public void deletePackage(Long id) {
        log.info("Soft-deleting rental package ID: {}", id);
        RentalPackage existing = getPackageById(id);
        existing.setActive(false);
        rentalPackageRepository.save(existing);
    }
}
