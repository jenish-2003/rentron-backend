package com.jcbbooking.repository;

import com.jcbbooking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByCustomerId(Long customerId);
    List<Booking> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findAllByContractorId(Long contractorId);
    List<Booking> findAllByContractorIdOrderByCreatedAtDesc(Long contractorId);
    List<Booking> findAllByDriverId(Long driverId);
    List<Booking> findAllByDriverIdOrderByCreatedAtDesc(Long driverId);
    Optional<Booking> findByBookingNumber(String bookingNumber);
    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    List<Booking> findAllByStatus(String status);
    List<Booking> findAllByDriverIdAndStatusIn(Long driverId, List<String> statuses);
    List<Booking> findAllByContractorIdAndStatusIn(Long contractorId, List<String> statuses);
    Optional<Booking> findFirstByDriverIdAndStatusInOrderByCreatedAtDesc(Long driverId, List<String> statuses);
    boolean existsByDriverIdAndStatusIn(Long driverId, List<String> statuses);
    List<Booking> findAllByStatusAndScheduledPickupAtLessThanEqual(String status, java.time.LocalDateTime leadThreshold);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Booking b SET b.driverId = :driverId, b.contractorId = :contractorId, b.status = :newStatus WHERE b.id = :bookingId AND b.status IN (:acceptableStatuses)")
    int assignDriverAtomically(@org.springframework.data.repository.query.Param("bookingId") Long bookingId,
                               @org.springframework.data.repository.query.Param("driverId") Long driverId,
                               @org.springframework.data.repository.query.Param("contractorId") Long contractorId,
                               @org.springframework.data.repository.query.Param("newStatus") String newStatus,
                               @org.springframework.data.repository.query.Param("acceptableStatuses") List<String> acceptableStatuses);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Booking b SET b.status = :newStatus WHERE b.id = :id AND b.status = :currentStatus")
    int transitionStatusIfMatches(@org.springframework.data.repository.query.Param("id") Long id,
                                  @org.springframework.data.repository.query.Param("newStatus") String newStatus,
                                  @org.springframework.data.repository.query.Param("currentStatus") String currentStatus);
}
