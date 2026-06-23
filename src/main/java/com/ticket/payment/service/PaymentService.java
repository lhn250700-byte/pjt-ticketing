package com.ticket.payment.service;

import com.ticket.payment.domain.Payment;
import com.ticket.payment.domain.PaymentMethod;
import com.ticket.payment.domain.PaymentStatus;
import com.ticket.payment.dto.PaymentRequest;
import com.ticket.payment.dto.PaymentResponse;
import com.ticket.payment.repository.PaymentRepository;
import com.ticket.reservation.domain.Reservation;
import com.ticket.reservation.repository.ReservationRepository;
import com.ticket.reservation.service.ReservationService;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ReservationService reservationService;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public PaymentResponse pay(PaymentRequest req) throws BadRequestException {
        Long userId = req.getUserId();
        Long scheduleId = req.getScheduleId();
        Long seatId = req.getSeatId();
        Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));
        log.info("[결제] userId={}, scheduleId={}, seatId={}", userId, scheduleId, seatId);

        // 결제 금액 비교 로직
        if (!seat.getPrice().equals(req.getAmount())) {
            log.warn("[결제 실패] 결제 금액 불일치. 좌석 금액={}, 요청 금액={}", seat.getPrice(), req.getAmount());
            throw new BadRequestException("결제 금액이 좌석 금액과 일치하지 않습니다.");
        }

        String userHoldKey = "concert:schedule:" + scheduleId + ":user:" + userId + ":hold";
        String isHeld = redisTemplate.opsForValue().get(userHoldKey);

        if (isHeld == null) {
            log.warn("[결제 실패] 5분 선점 시간이 만료되었거나 선점 내역이 없습니다. userId={}", userId);
            // 환불 로직 추가해야 함
            throw new IllegalStateException("예매 선점 시간(5분)이 만료되었거나 선점 내역이 없습니다. 결제가 취소됩니다.");
        }

        Long reservationId = reservationService.makeReservation(userId, scheduleId, seatId);
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다."));

        Payment payment = Payment.builder()
                .reservation(reservation)
                .amount(req.getAmount())
                .status(PaymentStatus.SUCCESS)
                .method(PaymentMethod.valueOf(req.getMethod()))
                .build();

        Payment newPay = paymentRepository.save(payment);
        log.info("[RDB 결제 장부 작성 완료] paymentId={}", newPay.getId());

        redisTemplate.delete(userHoldKey);
        log.info("[Redis 정리 완료] 유저 {}의 5분 임시 선점 키 삭제 성공", userId);

        return PaymentResponse.from(newPay);
    }
}
