package com.jcbbooking.repository;

import com.jcbbooking.model.OtpPurpose;
import com.jcbbooking.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByPhoneAndPurposeOrderByCreatedAtDesc(String phone, OtpPurpose purpose);
}
