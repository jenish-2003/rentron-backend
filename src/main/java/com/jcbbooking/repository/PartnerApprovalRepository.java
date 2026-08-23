package com.jcbbooking.repository;

import com.jcbbooking.model.PartnerApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerApprovalRepository extends JpaRepository<PartnerApproval, Long> {
    Optional<PartnerApproval> findByPhone(String phone);
    Optional<PartnerApproval> findByUserId(Long userId);
    List<PartnerApproval> findAllByPartnerType(String partnerType);
    List<PartnerApproval> findAllByContractorId(Long contractorId);
    List<PartnerApproval> findAllByStatus(String status);
    boolean existsByPhone(String phone);
}
