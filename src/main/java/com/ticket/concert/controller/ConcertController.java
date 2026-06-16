package com.ticket.concert.controller;

import java.util.List;

import com.ticket.concert.dto.ConcertDetailResponse;
import com.ticket.concert.dto.CursorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

	// 페이징 전
//	@GetMapping
//	public List<ConcertResponse> getAllConcerts() {
//
//	    log.info("GET /concerts 요청");
//
//	    List<ConcertResponse> concerts = concertService.getAllConcerts();
//
//	    log.info("GET /concerts 완료. count={}", concerts.size());
//
//	    return concerts;
//	}
	// 페이징 후
	@GetMapping
	public ResponseEntity<CursorResponse<ConcertResponse>> getConcerts(
			@RequestParam(required = false) Long nextId,
			@RequestParam(defaultValue = "10") int size
	) {
		return ResponseEntity.ok(concertService.getConcerts(nextId, size));
	}

	// 단건 조회
	@GetMapping("/{concertId}")
	public ResponseEntity<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
		return ResponseEntity.ok(concertService.getConcert(concertId));
	}
}
