package com.jcbbooking.controller;

import com.jcbbooking.model.*;
import com.jcbbooking.repository.SupportMessageRepository;
import com.jcbbooking.repository.SupportTicketRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.util.ApiResponse;
import com.jcbbooking.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to list support tickets for user ID: {}", userDetails.getId());
        if (userDetails.getUser().getRole() == Role.ADMIN) {
            return ResponseEntity.ok(ApiResponse.success("All support tickets retrieved", supportTicketRepository.findAll()));
        }
        List<SupportTicket> list = supportTicketRepository.findAllByUserIdOrderByCreatedAtDesc(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", list));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get support ticket ID {} for user ID {}", id, userDetails.getId());

        SupportTicket ticket = supportTicketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        // BACKEND AUTHORIZATION CHECK
        if (userDetails.getUser().getRole() != Role.ADMIN && !ticket.getUserId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied: You are not authorized to view this support ticket"));
        }

        List<SupportMessage> messages = supportMessageRepository.findAllByTicketIdOrderByCreatedAtAsc(ticket.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("ticket", ticket);
        response.put("messages", messages);

        return ResponseEntity.ok(ApiResponse.success("Support ticket retrieved", response));
    }

    @PostMapping("/tickets")
    @Transactional
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @RequestBody SupportTicket ticketRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to create support ticket for user ID: {}", userDetails.getId());

        ticketRequest.setId(null);
        ticketRequest.setUserId(userDetails.getId());
        ticketRequest.setTicketNumber("TICK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        if (ticketRequest.getStatus() == null) {
            ticketRequest.setStatus("OPEN");
        }
        if (ticketRequest.getPriority() == null) {
            ticketRequest.setPriority("MEDIUM");
        }

        SupportTicket savedTicket = supportTicketRepository.save(ticketRequest);

        // Record initial message if provided
        if (ticketRequest.getDescription() != null && !ticketRequest.getDescription().trim().isEmpty()) {
            SupportMessage initialMsg = SupportMessage.builder()
                    .ticketId(savedTicket.getId())
                    .senderUserId(userDetails.getId())
                    .senderRole(userDetails.getUser().getRole().name())
                    .message(ticketRequest.getDescription())
                    .build();
            supportMessageRepository.save(initialMsg);
        }

        return ResponseEntity.ok(ApiResponse.success("Support ticket raised successfully", savedTicket));
    }

    @PostMapping("/tickets/{id}/messages")
    @Transactional
    public ResponseEntity<ApiResponse<SupportMessage>> sendMessage(
            @PathVariable Long id,
            @RequestBody SupportMessage messageRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to send message on ticket ID: {}", id);

        SupportTicket ticket = supportTicketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        // BACKEND AUTHORIZATION CHECK
        if (userDetails.getUser().getRole() != Role.ADMIN && !ticket.getUserId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied: You cannot message on this ticket"));
        }

        if ("CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot add messages to a CLOSED support ticket"));
        }

        messageRequest.setId(null);
        messageRequest.setTicketId(id);
        messageRequest.setSenderUserId(userDetails.getId());
        messageRequest.setSenderRole(userDetails.getUser().getRole().name());

        SupportMessage savedMessage = supportMessageRepository.save(messageRequest);

        // Auto-update ticket status
        if (userDetails.getUser().getRole() == Role.ADMIN) {
            ticket.setStatus("IN_PROGRESS");
        } else if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            ticket.setStatus("REOPENED");
        }
        supportTicketRepository.save(ticket);

        // Broadcast real-time message via WebSocket
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "SUPPORT_CHAT_MESSAGE");
        wsPayload.put("ticketId", id);
        wsPayload.put("message", savedMessage);
        webSocketNotificationService.sendBookingOfferToUser(ticket.getUserId(), wsPayload);

        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", savedMessage));
    }

    @PostMapping("/tickets/{id}/reopen")
    @Transactional
    public ResponseEntity<ApiResponse<SupportTicket>> reopenTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to reopen ticket ID: {}", id);

        SupportTicket ticket = supportTicketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        // BACKEND AUTHORIZATION CHECK
        if (userDetails.getUser().getRole() != Role.ADMIN && !ticket.getUserId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied: Unauthorized to reopen ticket"));
        }

        if (!"RESOLVED".equalsIgnoreCase(ticket.getStatus()) && !"CLOSED".equalsIgnoreCase(ticket.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Only RESOLVED or CLOSED tickets can be reopened"));
        }

        ticket.setStatus("REOPENED");
        SupportTicket saved = supportTicketRepository.save(ticket);

        return ResponseEntity.ok(ApiResponse.success("Ticket reopened successfully", saved));
    }

    @PostMapping("/tickets/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<SupportTicket>> updateTicketStatus(
            @PathVariable Long id,
            @RequestParam String newStatus,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SupportTicket ticket = supportTicketRepository.findById(id).orElse(null);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        ticket.setStatus(newStatus.toUpperCase());
        if ("RESOLVED".equalsIgnoreCase(newStatus)) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if ("CLOSED".equalsIgnoreCase(newStatus)) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        SupportTicket saved = supportTicketRepository.save(ticket);
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated to " + newStatus, saved));
    }
}
