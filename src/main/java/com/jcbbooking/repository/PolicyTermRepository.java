package com.jcbbooking.repository;

import com.jcbbooking.model.PolicyTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyTermRepository extends JpaRepository<PolicyTerm, Long> {

    List<PolicyTerm> findAllByOrderByTargetAppAscPolicyTypeAsc();

    List<PolicyTerm> findByTargetApp(String targetApp);

    List<PolicyTerm> findByTargetAppInAndActiveTrue(List<String> targetApps);

    Optional<PolicyTerm> findFirstByTargetAppInAndPolicyTypeIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(
            List<String> targetApps, String policyType);
}
