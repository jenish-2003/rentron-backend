package com.jcbbooking.service;

import com.jcbbooking.model.RentalPackage;

import java.util.List;

public interface RentalPackageService {
    List<RentalPackage> getAllPackages();
    List<RentalPackage> getActivePackages(String vehicleCategory);
    RentalPackage getPackageById(Long id);
    RentalPackage createPackage(RentalPackage rentalPackage);
    RentalPackage updatePackage(Long id, RentalPackage rentalPackage);
    void deletePackage(Long id);
}
