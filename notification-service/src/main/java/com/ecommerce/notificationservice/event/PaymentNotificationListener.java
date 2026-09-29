package com.ecommerce.notificationservice.event;

import com.ecommerce.notificationservice.constant.KafkaConstant;
import com.ecommerce.notificationservice.dto.EmailDetails;
import com.ecommerce.notificationservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationListener.class);

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConstant.STATUS_PAYMENT_SUCCESSFUL,
            groupId = "${notification.kafka.consumer-group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentSuccessful(String message) {
        try {
            PaymentNotificationEvent event = objectMapper.readValue(message, PaymentNotificationEvent.class);
            if (event.recipient() == null || event.recipient().isBlank()) {
                log.warn("Skipping payment notification {} because recipient is missing", event.paymentId());
                return;
            }

            boolean paid = Boolean.TRUE.equals(event.isPayed());
            String result = emailService.sendSimpleMail(EmailDetails.builder()
                    .recipient(event.recipient())
                    .subject((paid ? "Xác nhận thanh toán" : "Xác nhận đặt hàng")
                            + " đơn hàng #" + event.orderId())
                    .msgBody("""
                            Xin chào,

                            %s cho đơn hàng #%s đã được ghi nhận.
                            Trạng thái: %s

                            Cảm ơn bạn đã mua sắm.
                            """.formatted(paid ? "Thanh toán" : "Đặt hàng",
                            event.orderId(), event.paymentStatus()))
                    .build());
            if (!"Mail Sent Successfully".equals(result)) {
                throw new IllegalStateException(result);
            }
            log.info("Payment notification sent for order {} to {}", event.orderId(), event.recipient());
        } catch (Exception ex) {
            log.error("Failed to process payment notification: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Payment notification processing failed", ex);
        }
    }

    public record PaymentNotificationEvent(
            Integer paymentId, Boolean isPayed, String paymentStatus,
            Integer orderId, Long userId, String recipient) {
    }
}
