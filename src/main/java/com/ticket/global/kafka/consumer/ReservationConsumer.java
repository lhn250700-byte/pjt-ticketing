package com.ticket.global.kafka.consumer;

import com.ticket.global.error.BusinessException;
import com.ticket.global.kafka.producer.SeatProducer;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationConsumer {
    private final ReservationRepository reservationRepository;
    private final SeatProducer seatProducer;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(
            topics = "concert.reservation.events",
            groupId = "concert-reservation-group",
            concurrency = "4"
    )
    @Transactional
    public void consumeReservationEvent(ConsumerRecord<String, String> record) {
        log.info("[Kafka Consumer] Reservation 메시지 수신 완료 | Partition: {}, Offset: {}, Key(UserId): {}", record.partition(), record.offset(), record.key());
        Long reservationId = Long.parseLong(record.value());
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."));
        Long userId = reservation.getUser().getId();
        Long seatId = reservation.getSeat().getId();
        Long scheduleId = reservation.getSchedule().getId();

        try {
            reservation.reserve();
            seatProducer.sendSeatEvent(userId, scheduleId, seatId);
        } catch (Exception e) {
            log.error("[2단계 오류] 예약 상태 확정 중 예외 발생. 결제 환불 및 자원 원복을 시작합니다. 원인: {}", e.getMessage());
            reservation.fail(); // reservation PENDING -> FAILED

            // redis 선점 관련 키 제거
            String userHoldKey = "concert:schedule:" + reservation.getSchedule().getId() + ":user:" + userId + ":hold";
            String seatStatusKey = "concert:schedule:" + reservation.getSchedule().getId() + ":seat:" + userId + ":status";
            redisTemplate.delete(userHoldKey);
            redisTemplate.delete(seatStatusKey);

            // 잔여 좌석 수 + 1로 원복
            String countKey = "concert:schedule:" + reservation.getSchedule().getId() + ":seat:count";
            redisTemplate.opsForValue().increment(countKey, 1);
            log.info("[2단계 보상 처리] Redis 가선점 자원 반환 완료");

            throw new RuntimeException(e);
        }
    }
}
