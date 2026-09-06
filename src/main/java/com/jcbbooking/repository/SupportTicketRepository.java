package com.jcbbooking.repository;

import com.jcbbooking.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SupportTicket> findByIdAndUserId(Long id, Long userId);
}
