package com.ticket.concert.service;

import java.util.List;

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
	
	@Transactional
	public Long createConcert(ConcertCreate req) {

	    log.info("공연 생성 시작. title={}", req.getTitle());

	    Concert concert = Concert.builder()
	            .title(req.getTitle())
	            .description(req.getDescription())
	            .runtime(req.getRuntime())
	            .build();

	    Concert newConcert = concertRepository.save(concert);

	    log.info("공연 생성 완료. concertId={}", newConcert.getId());

	    return newConcert.getId();
	}
	
	public List<ConcertResponse> getAllConcerts() {
		return concertRepository.findAll()
				.stream()
				.map(ConcertResponse::from)
				.toList();
	}
}
