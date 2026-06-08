package com.ticket.reservation.controller;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ticket.reservation.dto.MakeReservationRequest;
import com.ticket.reservation.service.ReservationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reservations")
@Slf4j
public class ReservationController {
	private final ReservationService reservationService;

//	=============================================================================
//	기존 : public ResponseEntity<Long> makeReservation(@Valid @RequestBody MakeReservationRequest req)
//	현재 :
//	=============================================================================
	@PostMapping
	public ResponseEntity<Long> makeReservation(@RequestHeader("User-Id") Long userId, @RequestParam("scheduleId") Long scheduleId, @Valid @RequestBody MakeReservationRequest req)
	        throws BadRequestException {

	    log.info("POST /reservations 요청. userId={}, scheduleId={}, seatId={}",
	            req.getUserId(), req.getScheduleId(), req.getSeatId());

	    Long reservationId = reservationService.makeReservation(req);

	    log.info("POST /reservations 완료. reservationId={}", reservationId);

	    return ResponseEntity.status(HttpStatus.CREATED).body(reservationId);
	}
}
