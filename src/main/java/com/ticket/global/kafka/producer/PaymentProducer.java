package com.ticket.global.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendPaymentEvent(Long userId, Long reservationId, String token, Long amount, String method) {
        String topicName = "concert.payment.events";

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", reservationId);
            payload.put("queueToken", token);
            payload.put("amount", amount);
            payload.put("method", method);

            String messageKey = userId.toString();
            String messageValue = toJsonString(payload);
            log.info("[Kafka Producer] 유저 {}의 결제 요청을 {} 토픽으로 발행합니다.", userId, messageKey);
            kafkaTemplate.send(topicName, messageKey, messageValue);
        } catch (Exception e) {
            log.error("[Kafka Producer] 이벤트 발행 중 예외 발생 : ", e);
            throw new RuntimeException("결제 요청 처리 중 서버 오류가 발생했습니다.");
        }
    }

    private String toJsonString(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json 직렬화 실패");
        }
    }
}
