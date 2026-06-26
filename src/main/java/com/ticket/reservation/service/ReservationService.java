package com.ticket.reservation.service;

import java.time.LocalDateTime;

import com.ticket.reservation.kafka.producer.ReservationProducer;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.global.error.BusinessException;
import com.ticket.reservation.domain.Reservation;
import com.ticket.reservation.domain.ReservationStatus;
import com.ticket.reservation.dto.MakeReservationRequest;
import com.ticket.reservation.repository.ReservationRepository;
import com.ticket.schedule.domain.Schedule;
import com.ticket.schedule.repository.ScheduleRepository;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.repository.SeatRepository;
import com.ticket.user.domain.User;
import com.ticket.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final ReservationProducer reservationProducer;
	private final SeatRepository seatRepository;
	private final StringRedisTemplate redisTemplate;
	
	@Transactional
	public Long makeReservation(Long userId, Long scheduleId, Long seatId) {

	    log.info("예약 생성 시작. userId={}, scheduleId={}, seatId={}",
	            userId, scheduleId, seatId);

	    User user = userRepository.findById(userId)
	            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 User Id 입니다."));

	    Seat seat = seatRepository.findById(seatId)
	            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 Seat Id 입니다."));

		Schedule schedule = seat.getSchedule();

	    if (!schedule.getId().equals(scheduleId)) {
	        log.warn("예약 실패 - 좌석/스케줄 불일치. userId={}, scheduleId={}, seatId={}",
					userId, scheduleId, seatId);
	        throw new BusinessException(HttpStatus.BAD_REQUEST, "유효하지 않는 좌석입니다.");
	    }

	    if (seat.getIsReserved()) {
	        log.warn("예약 실패 - 이미 예약된 좌석. seatId={}", seatId);
	        throw new BusinessException(HttpStatus.CONFLICT, "예약된 좌석입니다.");
	    }

	    Reservation reservation = Reservation.builder()
	            .user(user)
	            .schedule(schedule)
	            .seat(seat)
	            .status(ReservationStatus.CONFIRMED)
	            .build();

	    Reservation newRes = reservationRepository.save(reservation);

	    seat.reservation();

	    log.info("예약 생성 성공. reservationId={}", newRes.getId());

	    return newRes.getId();
	}

	public void requestSeatHold(MakeReservationRequest req) {
		// 보안 검증) 이 유저가 진짜 active 방에 있는지 확인
		String activeKey = "concert:queue:active:" + req.getScheduleId();
		String queueValue = req.getQueueToken() + ":" + req.getUserId();
		Boolean isActive = redisTemplate.opsForSet().isMember(activeKey, queueValue);

		if (Boolean.FALSE.equals(isActive)) {
			log.warn("Active 상태가 아닌 유저의 접근. userId={}, scheduleId={}", req.getUserId(), req.getScheduleId());
			throw new BusinessException(HttpStatus.FORBIDDEN, "대기열을 통과하지 않은 비정상 접근입니다.");
		}

		reservationProducer.sendReservationEvent(req.getUserId(), req.getScheduleId(), req.getSeatId(), req.getQueueToken());
		log.info("[임시 선점 요청 접수 완료] userId={}, seatId={}", req.getUserId(), req.getSeatId());
	}
}
