package com.ticket.concert.controller;

import com.ticket.concert.dto.*;
import com.ticket.concert.service.ConcertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
		log.info("GET /concerts 요청");
		CursorResponse<ConcertResponse> response = concertService.getConcerts(nextId, size);
		log.info("GET /concerts 완료. count={}", response.content().size());

		return ResponseEntity.ok(response);
	}

	// 단건 조회
	@GetMapping("/{concertId}")
	public ResponseEntity<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
		log.info("GET /concerts/{} 요청", concertId);
		ConcertDetailResponse res = concertService.getConcert(concertId);
		log.info("GET /concerts/{} 완료", concertId);
		return ResponseEntity.ok(res);
	}

	// 수정
	@PatchMapping("/{concertId}")
	public ResponseEntity<ConcertResponse> updateConcert(@PathVariable Long concertId, @RequestBody ConcertUpdate req) {
		log.info("PATCH /concerts/{} 요청", concertId);
		ConcertResponse response = concertService.updateConcert(concertId, req);
		log.info("PATCH /concerts/{} 완료", concertId);
		return ResponseEntity.ok(response);
	}

	// 삭제
	@DeleteMapping("/{concertId}")
	public ResponseEntity<String> deleteConcert(@PathVariable Long concertId) {
		log.info("DEL /concerts/{} 요청", concertId);
		concertService.deleteConcert(concertId);
		log.info("DEL /concerts/{} 완료", concertId);
		return ResponseEntity.ok(concertId + " 삭제 완료");
	}
}
