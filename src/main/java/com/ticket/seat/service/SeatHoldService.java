package com.ticket.seat.service;

import com.ticket.global.error.BusinessException;
import com.ticket.global.kafka.producer.PaymentProducer;
import com.ticket.reservation.domain.Reservation;
import com.ticket.reservation.domain.ReservationStatus;
import com.ticket.reservation.dto.MakeReservationRequest;
import com.ticket.reservation.repository.ReservationRepository;
import com.ticket.schedule.domain.Schedule;
import com.ticket.seat.domain.Seat;
import com.ticket.user.domain.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatHoldService {
    private  final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> holdSeatScript;
    private final PaymentProducer paymentProducer;
    @PersistenceContext
    private final EntityManager em;

    public void holdSeat(MakeReservationRequest req) {
        Long scheduleId = req.getScheduleId();
        Long seatId = req.getSeatId();
        Long userId = req.getUserId();
        Long amount = req.getAmount();
        String method = req.getMethod();
        String queueToken = req.getQueueToken();

        List<String> keys = List.of(
                "concert:schedule:" + scheduleId + ":seat:" + seatId + ":status", // KEYS[1]
                "concert:schedule:" + scheduleId + ":user:" + userId + ":hold",   // KEYS[2]
                "concert:schedule:" + scheduleId + ":seat:count"                  // KEYS[3]
        );
        String requestedQuantity = "1"; // 인당 1매 선점 가정
        String ttlSeconds = "300"; // 5분 임시 선점 기한 TTL 설정

        Object[] args = new Object[]{
                requestedQuantity,     // ARGV[1] : 요청 수량 (보통 1)
                ttlSeconds,            // ARGV[2] : 만료 시간 (5분 = 300초)
                String.valueOf(userId) // ARGV[3] : 좌석에 박아둘 주인 유저 ID
        };

        Long result = redisTemplate.execute(holdSeatScript, keys, args);

        if (result == null || result == -1) {
            log.error("[선점 시스템 에러] Redis에 해당 스케줄의 재고 정보가 존재하지 않습니다. scheduleId={}", scheduleId);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "예매 요청 처리 중 서버 오류가 발생했습니다.");
        }

        if (result == -2) {
            log.warn("[선점 실패] 중복 예매 요청 차단. scheduleId={}, userId={}", scheduleId, userId);
            throw new BusinessException(HttpStatus.CONFLICT, "이미 이 공연의 좌석을 선점하셨거나 예매 진행 중입니다.");
        }

        if (result == -3) {
            log.warn("[선점 실패] 잔여 좌석 부족. scheduleId={}, userId={}", scheduleId, userId);
            throw new BusinessException(HttpStatus.CONFLICT, "선택하신 회차의 잔여 좌석이 모두 매진되었습니다.");
        }

        if (result == 0) {
            log.warn("[선점 실패] 매진 완료. scheduleId={}, userId={}", scheduleId, userId);
            throw new BusinessException(HttpStatus.CONFLICT, "선택하신 회차의 잔여 좌석이 모두 매진되었습니다.");
        }

        log.info("[루아 스크립트 임시 선점 성공] 유저 {} -> 스케줄 {} 잔여 재고 1 차감 완료", userId, scheduleId);

        try {
            Long reservationId = pendingReservation(userId, scheduleId, seatId);
            paymentProducer.sendPaymentEvent(userId, reservationId, queueToken, amount, method);
        } catch (Exception e) {
            log.error("[보상 트랜잭션 롤백] 예외 발생으로 Redis 선점을 취소합니다. 원인: {}", e.getMessage());

            try {
                // 유저 선점 키 및 좌석 상태 키 삭제
                redisTemplate.delete(keys.get(1));
                redisTemplate.delete(keys.get(0));

                // 임시 좌석 수 + 1 원복
                redisTemplate.opsForValue().increment(keys.get(2), 1);
                log.info("[보상 트랜잭션 완료]");
            } catch (Exception ex) {
                log.error("[보상 트랜잭션 실패] Redis 보상 처리 중 에러 발생. 수동 확인 필요. userId={}, seatId={}", userId, seatId, ex);
            }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "예약 처리 중 오류가 발생했습니다. 다시 시도해 주세요.");
        }
    }

    @Transactional
    public Long pendingReservation(Long userId, Long scheduleId, Long seatId) {
        User userProxy = em.getReference(User.class, userId);
        Schedule scheduleProxy = em.getReference(Schedule.class, scheduleId);
        Seat seatProxy = em.getReference(Seat.class, seatId);

        Reservation reservation = Reservation.builder()
                .user(userProxy)
                .schedule(scheduleProxy)
                .seat(seatProxy)
                .status(ReservationStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return saved.getId();
    }
}
