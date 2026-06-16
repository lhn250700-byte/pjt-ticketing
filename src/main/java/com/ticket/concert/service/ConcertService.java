package com.ticket.concert.service;

import java.util.List;
import java.util.Optional;

import com.ticket.concert.dto.ConcertDetailResponse;
import com.ticket.concert.dto.CursorResponse;
import com.ticket.schedule.dto.ScheduleResponse;
import com.ticket.schedule.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.concert.domain.Concert;
import com.ticket.concert.dto.ConcertCreate;
import com.ticket.concert.dto.ConcertResponse;
import com.ticket.concert.repository.ConcertRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConcertService {
	private final ConcertRepository concertRepository;
	private final ScheduleRepository scheduleRepository;
	
	@Transactional
	public Long createConcert(ConcertCreate req) {

	    log.info("공연 생성 시작. title={}", req.getTitle());

	    Concert concert = Concert.builder()
	            .title(req.getTitle())
	            .description(req.getDescription())
				.venue(req.getVenue())
	            .runtime(req.getRuntime())
	            .build();

	    Concert newConcert = concertRepository.save(concert);

	    log.info("공연 생성 완료. concertId={}", newConcert.getId());

	    return newConcert.getId();
	}

	// 페이징 전 GET concerts/
	public List<ConcertResponse> getAllConcerts() {
		return concertRepository.findAll()
				.stream()
				.map(ConcertResponse::from)
				.toList();
	}

	// 페이징 GET concerts/
	public CursorResponse<ConcertResponse> getConcerts(Long concertId, int size) {
		List<Concert> concerts = (concertId == null) ? concertRepository.findTop11ByOrderByIdDesc() : concertRepository.findTop11ByIdLessThanOrderByIdDesc(concertId);

		// 다음 페이지 존재 유무 확인
		boolean hasNext = concerts.size() > size;

		// 실제 반환 데이터
		List<Concert> content = hasNext ? concerts.subList(0, size) : concerts;

		// 다음 페이지 조회 시 사용할 커서 추출
		Long nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).getId();

		// List<ConcertResponse> -> CursorResponse로 변환
		List<ConcertResponse> response = content.stream().map(ConcertResponse::from).toList();

		return new CursorResponse<>(response, nextCursor, hasNext);
	}

	// GET /concerts/{concertId}
	public ConcertDetailResponse getConcert(Long concertId) {
		Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));

		List<ScheduleResponse> schedules = scheduleRepository.findByConcertIdOrderByStartAsc(concertId)
				.stream()
				.map(ScheduleResponse::from)
				.toList();

		return ConcertDetailResponse.from(concert, schedules);
	}
}
