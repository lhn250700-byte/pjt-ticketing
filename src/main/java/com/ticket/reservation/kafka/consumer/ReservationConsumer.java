package com.ticket.reservation.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.global.error.BusinessException;
import com.ticket.reservation.dto.MakeReservationRequest;
import com.ticket.reservation.service.ReservationService;
import com.ticket.seat.service.SeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConsumer {
//    private final ReservationService reservationService;
    private final StringRedisTemplate redisTemplate;
    private final SeatHoldService seatHoldService;

    @KafkaListener(
            topics = "concert.reservation.events",
            groupId = "concert-reservation-group"
    )
    public void consumeReservationEvent(ConsumerRecord<String, String> record) {
        log.info("[Kafka Consumer] 메시지 수신 완료 | Partition: {}, Offset: {}, Key(UserId): {}", record.partition(), record.offset(), record.key());
        Long userId = Long.parseLong(record.key());
        // 카프카에서 꺼내온 JSON 문자열 -> 자바 객체 역직렬화
        Map<String, Object> payload = fromJson(record.value());
        Long scheduleId = Long.valueOf(payload.get("scheduleId").toString());
        Long seatId = Long.valueOf(payload.get("seatId").toString());
        String queueToken = payload.get("queueToken").toString();
        // active 방에서 유저 삭제
        String activeKey = "concert:queue:active:" + scheduleId;
        String queueValue = queueToken + ":" + userId;

        try {
            log.info("[Kafka Consumer] 데이터 파싱 성공 | 유저: {}, 일정: {}, 좌석: {}", userId, scheduleId, seatId);

//            Long reservationId = reservationService.makeReservation(userId, scheduleId, seatId);
//            log.info("[Kafka Consumer] 메인 DB 반영 성공 | 최종 예매 완료 (예매 ID: {})", reservationId);
            seatHoldService.holdSeat(scheduleId, userId, seatId);
            log.info("[Kafka Consumer] Redis 임시 선점 성공 | 유저 {} 선점권 획득", userId);

            // 임시 선점 성공 => active방 + 대기실 유저 토큰 정리
            redisTemplate.opsForSet().remove(activeKey, queueValue);
            redisTemplate.delete("concert:user:" + userId + ":schedule:" + scheduleId);
            log.info("[Kafka Consumer] 유저 {} 예매 완료로 인한 대기열 토큰 정리 완료", userId);
        } catch (BusinessException e) {
        		if (e.getStatus().is5xxServerError()) {
        			throw e;
        		}
        		
            log.info("[Kafka Consumer] 좌석 선점 실패 유저 대기열 정리 : {}", userId);
            redisTemplate.opsForSet().remove(activeKey, queueValue);
            redisTemplate.delete("concert:user:" + userId + ":schedule:" + scheduleId);
        } catch (Exception e) {
            log.error("예매 처리 실패", e);
            throw new RuntimeException("[Kafka Consumer] 최종 예매 처리 중 장애 발생: ", e);
        }
    }

    @KafkaListener(
            topics = "concert.reservation.events.dlt",
            groupId = "concert-reservation-dlt-group"
    )
    public void listenDLQ(String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.OFFSET) Long offset) {
        log.error("======= [DLQ 에러 감지] =======");
        log.error("격리된 메시지 본문 : {}", message);
        log.error("원본 발생 토픽 명 : {}", topic);
        log.error("카프카 오프셋 번호 : {}", offset);
        log.error("=============================");
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
