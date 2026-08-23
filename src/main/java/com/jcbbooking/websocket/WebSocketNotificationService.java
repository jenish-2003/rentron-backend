package com.jcbbooking.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendBookingOfferToUser(Long userId, Map<String, Object> offerPayload) {
        log.info("Sending STOMP booking offer to user ID {}", userId);
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/booking-offers",
                    (Object) offerPayload
            );
            // Also send on general topic for active listeners
            messagingTemplate.convertAndSend("/topic/booking-offers/" + userId, (Object) offerPayload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket booking offer to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendOfferCancelledToUser(Long userId, Map<String, Object> cancelPayload) {
        log.info("Sending STOMP offer cancellation to user ID {}", userId);
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/booking-offers",
                    (Object) cancelPayload
            );
            messagingTemplate.convertAndSend("/topic/booking-offers/" + userId, (Object) cancelPayload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket offer cancellation to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendAdminAssignmentFailedNotification(Map<String, Object> adminPayload) {
        log.warn("Sending STOMP admin notification for failed booking assignment");
        try {
            messagingTemplate.convertAndSend("/topic/admin/booking-alerts", (Object) adminPayload);
        } catch (Exception e) {
            log.error("Failed to send WebSocket admin alert: {}", e.getMessage());
        }
    }
}
