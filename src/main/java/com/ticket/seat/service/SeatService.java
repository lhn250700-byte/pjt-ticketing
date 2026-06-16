package com.ticket.seat.service;

import com.ticket.seat.dto.SeatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

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
}
