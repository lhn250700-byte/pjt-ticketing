package com.ticket.global.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.global.error.BusinessException;
import com.ticket.global.kafka.producer.ReservationProducer;
import com.ticket.payment.domain.Payment;
import com.ticket.payment.domain.PaymentMethod;
import com.ticket.payment.domain.PaymentStatus;
import com.ticket.payment.dto.PaymentRequest;
import com.ticket.payment.repository.PaymentRepository;
import com.ticket.payment.service.PaymentService;
import com.ticket.reservation.domain.Reservation;
import com.ticket.reservation.repository.ReservationRepository;
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
public class PaymentConsumer {
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationProducer reservationProducer;
    private final StringRedisTemplate redisTemplate;
    private final PaymentService paymentService;

    @KafkaListener(
            topics = "concert.payment.events",
            groupId = "concert-payment-group",
            concurrency = "4"
    )
    @Transactional
    public void consumePaymentEvent(ConsumerRecord<String ,String> record) {
        log.info("[Kafka Consumer] Payment 메시지 수신 완료 | Partition: {}, Offset: {}, Key(UserId): {}", record.partition(), record.offset(), record.key());
        // 카프카에서 꺼내온 JSON 문자열 -> 자바 객체 역직렬화
        Map<String, Object> payload = fromJson(record.value());
        String queueToken = payload.get("queueToken").toString();
        Long userId = Long.parseLong(record.key());
        Long reservationId = Long.parseLong(payload.get("reservationId").toString());
        Long amount = Long.parseLong(payload.get("amount").toString());
        String method = payload.get("method").toString();
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."));
        Long scheduleId = reservation.getSchedule().getId();
        Long seatPrice = reservation.getSeat().getPrice();
        Long seatId = reservation.getSeat().getId();


        try {
            // payment insert
            PaymentRequest req = PaymentRequest.builder()
                    .userId(userId)
                    .scheduleId(scheduleId)
                    .seatId(seatId)
                    .reservationId(reservationId)
                    .amount(amount)
                    .method(method)
                    .build();
            paymentService.payment(req);

            // active 방에서 유저 삭제
            String activeKey = "concert:queue:active:" + scheduleId;
            String queueValue = queueToken + ":" + userId;
            redisTemplate.opsForSet().remove(activeKey, queueValue);
            redisTemplate.delete("concert:user:" + userId + ":schedule:" + scheduleId);
            log.info("[Kafka Consumer] 유저 {} 예매 완료로 인한 대기열 토큰 정리 완료", userId);

            // 결제 토픽 발행
            reservationProducer.sendReservationEvent(userId, reservationId);
        } catch (Exception e) {
            log.error("[1단계 오류] 결제 처리 중 에외 발생. 파이프라인을 중단하고 자원을 원복합니다. 원인: {}", e.getMessage());
            reservation.cancel(); // reservation PENDING -> CANCELED

            // redis 선점 관련 키 제거
            String userHoldKey = "concert:schedule:" + scheduleId + ":user:" + userId + ":hold";
            String seatStatusKey = "concert:schedule:" + scheduleId + ":seat:" + userId + ":status";
            redisTemplate.delete(userHoldKey);
            redisTemplate.delete(seatStatusKey);

            // 잔여 좌석 수 + 1로 원복
            String countKey = "concert:schedule:" + scheduleId + ":seat:count";
            redisTemplate.opsForValue().increment(countKey, 1);
            log.info("[1단계 보상 처리] Redis 가선점 자원 반환 완료");

            throw new RuntimeException(e);
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