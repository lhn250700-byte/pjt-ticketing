package com.ticket.reservation.service;

import java.time.LocalDateTime;

import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
@Slf4j
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final UserRepository userRepository;
	private final ScheduleRepository scheduleRepository;
	private final SeatRepository seatRepository;
	
	@Transactional
	public Long makeReservation(MakeReservationRequest req) throws BadRequestException {

	    log.info("예약 생성 시작. userId={}, scheduleId={}, seatId={}",
	            req.getUserId(), req.getScheduleId(), req.getSeatId());

	    User user = userRepository.findById(req.getUserId())
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 User Id 입니다."));

	    Schedule schedule = scheduleRepository.findById(req.getScheduleId())
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Schedule Id 입니다."));

	    Seat seat = seatRepository.findById(req.getSeatId())
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Seat Id 입니다."));

	    if (!seat.getSchedule().getId().equals(schedule.getId())) {
	        log.warn("예약 실패 - 좌석/스케줄 불일치. userId={}, scheduleId={}, seatId={}",
	                req.getUserId(), req.getScheduleId(), req.getSeatId());
	        throw new BadRequestException("유효하지 않는 좌석입니다.");
	    }

	    if (seat.getIsReserved()) {
	        log.warn("예약 실패 - 이미 예약된 좌석. seatId={}", req.getSeatId());
	        throw new BadRequestException("이미 예약된 좌석입니다.");
	    }

	    if (LocalDateTime.now().isBefore(schedule.getBookOpen())) {
	        log.warn("예약 실패 - 티켓 오픈 전. scheduleId={}, bookOpen={}",
	                req.getScheduleId(), schedule.getBookOpen());
	        throw new BadRequestException("아직 티케팅 오픈 시간이 아닙니다.");
	    }

	    if (LocalDateTime.now().isAfter(schedule.getStart())) {
	        log.warn("예약 실패 - 공연 시작 이후. scheduleId={}, startTime={}",
	                req.getScheduleId(), schedule.getStart());
	        throw new BadRequestException("이미 시작했거나 종료된 공연입니다.");
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
}
