package com.ticket.seat.service;

import com.ticket.global.error.BusinessException;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.dto.SeatResponse;
import com.ticket.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {
	private final SeatRepository seatRepository;
	
	// GET /schedules/{scheduleId}/seats
	public List<SeatResponse> getSeats(Long scheduleId) {
		return seatRepository.findByScheduleId(scheduleId)
				.stream()
				.map(SeatResponse::from)
				.toList();
	}

	@Transactional
	public void seatReservation(Long seatId) {
		Seat seat = seatRepository.findById(seatId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 좌석입니다."));
		seat.reserve();
	}
}
