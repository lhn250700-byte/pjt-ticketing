package com.ticket.global.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.global.error.BusinessException;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeatConsumer {
    private final SeatRepository seatRepository;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(
            topics = "concert.seat.events",
            groupId = "concert-seat-group",
            concurrency = "4"
    )
    @Transactional
    public void consumeSeatEvent(ConsumerRecord<String, String> record) {
        log.info("[Kafka Consumer] Seat 메시지 수신 완료 | Partition: {}, Offset: {}, Key(UserId): {}", record.partition(), record.offset(), record.key());
        Long userId = Long.parseLong(record.key());
        Map<String, Object> payload = fromJson(record.value());
        Long scheduleId = Long.valueOf(payload.get("scheduleId").toString());
        Long seatId = Long.valueOf(payload.get("seatId").toString());
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 좌석입니다."));

        try {
            // 예약 관련 에러 처리 해야 함
            seat.reserve();
        } catch (Exception e) {
            log.error("[3단계 오류] 최종 좌석 상태 반영 중 에외 발생. 파이프라인을 중단하고 자원을 원복합니다. 원인: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            // redis 선점 관련 키 제거
            String userHoldKey = "concert:schedule:" + scheduleId + ":user:" + userId + ":hold";
            String seatStatusKey = "concert:schedule:" + scheduleId + ":seat:" + userId + ":status";
            redisTemplate.delete(userHoldKey);
            redisTemplate.delete(seatStatusKey);
        }
    }

    private Map<String, Object> fromJson (String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Json 파싱 실패");
        }
    }
}
