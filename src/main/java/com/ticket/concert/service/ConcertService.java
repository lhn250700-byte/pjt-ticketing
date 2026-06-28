package com.ticket.concert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.concert.domain.Concert;
import com.ticket.concert.dto.*;
import com.ticket.concert.repository.ConcertRepository;
import com.ticket.global.error.BusinessException;
import com.ticket.schedule.dto.ScheduleResponse;
import com.ticket.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConcertService {
	private final ConcertRepository concertRepository;
	private final ScheduleRepository scheduleRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

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
		String concertKey = "concert:" + concertId;
		String cachedData = redisTemplate.opsForValue().get(concertKey);

		if (cachedData != null) {
			try {
				return objectMapper.readValue(cachedData, ConcertDetailResponse.class);
			} catch (Exception e) {
				log.error("[Concert] 캐시 데이터 가져오기 실패", e);
				redisTemplate.delete(concertKey);
			}
		}

		Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 콘서트입니다."));

		List<ScheduleResponse> schedules = scheduleRepository.findByConcertIdOrderByStartAsc(concertId)
				.stream()
				.map(ScheduleResponse::from)
				.toList();

		ConcertDetailResponse response = ConcertDetailResponse.from(concert, schedules);

		// redis 저장
        try {
            redisTemplate.opsForValue().set(concertKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(10));
        } catch (JsonProcessingException e) {
            log.error("[Concert] 캐싱 실패", e);
        }

        return response;
	}

	// PATCH /concerts/{concertId}
	@Transactional
	public ConcertResponse updateConcert(Long concertId, ConcertUpdate req) {
		Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 콘서트입니다."));
		concert.update(req.getTitle(), req.getDescription(), req.getVenue(), req.getRuntime());
		Concert savedConcert = concertRepository.save(concert);
		String concertKey = "concert:" + concertId;
		String cachedData = redisTemplate.opsForValue().get(concertKey);

		ConcertResponse response = ConcertResponse.from(savedConcert);

		if (cachedData != null) {
            try {
				redisTemplate.delete(concertKey);
                redisTemplate.opsForValue().set(concertKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(10));
            } catch (JsonProcessingException e) {
				log.error("[Concert] 캐싱 실패", e);
            }
        }

		return response;
	}

	@Transactional
	public void deleteConcert(Long concertId) {
		Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 콘서트입니다."));
		String concertKey = "concert:" + concertId;
		String cachedData = redisTemplate.opsForValue().get(concertKey);
		concertRepository.delete(concert);
		if (cachedData != null) redisTemplate.delete(concertKey);
	}
}
