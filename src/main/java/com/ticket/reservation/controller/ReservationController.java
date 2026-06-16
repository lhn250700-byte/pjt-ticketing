package com.ticket.reservation.controller;

import com.ticket.reservation.kafka.producer.ReservationProducer;
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
	private final ReservationProducer reservationProducer;

//	=============================================================================
//	기존 : public ResponseEntity<Long> makeReservation(@Valid @RequestBody MakeReservationRequest req)
//	현재 : public ResponseEntity<Long> makeReservation(@RequestHeader("User-Id") Long userId, @RequestParam("scheduleId") Long scheduleId, @Valid @RequestBody MakeReservationRequest req)
//	=============================================================================
	@PostMapping
	public ResponseEntity<String> makeReservation(@RequestHeader("User-Id") Long userId, @RequestHeader("Queue-Token") String queueToken, @RequestParam("scheduleId") Long scheduleId, @Valid @RequestBody MakeReservationRequest req)
	        throws BadRequestException {

	    log.info("POST /reservations 요청. userId={}, scheduleId={}, seatId={}",
	            userId, scheduleId, req.getSeatId());

//	    Long reservationId = reservationService.makeReservation(userId, scheduleId, req.getSeatId())

//	    log.info("POST /reservations 완료. reservationId={}", reservationId);
		reservationProducer.sendReservationEvent(userId, scheduleId, req.getSeatId(), queueToken);
//	    return ResponseEntity.status(HttpStatus.CREATED).body(reservationId);
		return ResponseEntity.accepted().body("예매 요청이 성공적으로 접수되었습니다.");
	}
}
