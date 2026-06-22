package com.ticket.seat.controller;

import com.ticket.seat.dto.SeatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticket.seat.service.SeatService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SeatController {
	private final SeatService seatService;

	// 전체 조회
	@GetMapping("/schedules/{scheduleId}/seats")
	public List<SeatResponse> getSeats(@PathVariable Long scheduleId) {
		log.info("GET /schedules/{}/seats 요청", scheduleId);
		List<SeatResponse> seats = seatService.getSeats(scheduleId);
		log.info("GET /schedules/{}/seats 완료. count={}", scheduleId, seats.size());
		return seats;
	}
}
