package com.ticket.reservation.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendReservationEvent(Long userId, Long scheduleId, Long seatId, String queueToken) {
        String topicName = "concert.reservation.events";

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("scheduleId", scheduleId);
            payload.put("seatId", seatId);
            payload.put("queueToken", queueToken);

            String messageValue = toJsonString(payload);
            String messageKey = userId.toString();

            log.info("[Kafka Producer] 유저 {}의 예매 요청을 {} 토픽으로 발행합니다.", userId, messageKey);
            kafkaTemplate.send(topicName, messageKey, messageValue);
        } catch (Exception e) {
            log.error("[Kafka Producer] 이벤트 발행 중 예외 발생 : ", e);
            throw new RuntimeException("예매 요청 처리 중 서버 오류가 발생했습니다.");
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
