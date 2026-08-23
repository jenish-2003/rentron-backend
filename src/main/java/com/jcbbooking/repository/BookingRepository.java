package com.jcbbooking.repository;

import com.jcbbooking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByCustomerId(Long customerId);
    List<Booking> findAllByContractorId(Long contractorId);
    List<Booking> findAllByDriverId(Long driverId);
    Optional<Booking> findByBookingNumber(String bookingNumber);
    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);
    List<Booking> findAllByStatus(String status);
    List<Booking> findAllByDriverIdAndStatusIn(Long driverId, List<String> statuses);
    List<Booking> findAllByContractorIdAndStatusIn(Long contractorId, List<String> statuses);
}
