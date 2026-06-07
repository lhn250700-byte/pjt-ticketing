package com.ticket.concert.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticket.concert.dto.ConcertCreate;
import com.ticket.concert.dto.ConcertResponse;
import com.ticket.concert.service.ConcertService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/concerts")
@Slf4j
public class ConcertController {
	private final ConcertService concertService;
	
	@PostMapping
	public ResponseEntity<Long> createConcert(@Valid @RequestBody ConcertCreate req) {

	    log.info("POST /concerts 요청. title={}", req.getTitle());

	    Long concertId = concertService.createConcert(req);

	    log.info("POST /concerts 완료. concertId={}", concertId);

	    return ResponseEntity.status(HttpStatus.CREATED).body(concertId);
	}

	@GetMapping
	public List<ConcertResponse> getAllConcerts() {

	    log.info("GET /concerts 요청");

	    List<ConcertResponse> concerts = concertService.getAllConcerts();

	    log.info("GET /concerts 완료. count={}", concerts.size());

	    return concerts;
	}
}
