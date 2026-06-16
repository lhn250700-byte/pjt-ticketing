package com.ticket.schedule.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.concert.domain.Concert;
import com.ticket.concert.repository.ConcertRepository;
import com.ticket.schedule.domain.Schedule;
import com.ticket.schedule.dto.ScheduleCreateRequest;
import com.ticket.schedule.repository.ScheduleRepository;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.domain.SeatGrade;
import com.ticket.seat.repository.SeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScheduleService {
	private final ScheduleRepository scheduleRepository;
	private final ConcertRepository concertRepository;
	private final SeatRepository seatRepository;
	
	@Transactional
	public Long createSchedule(ScheduleCreateRequest req) {

	    log.info("스케줄 생성 시작. concertId={}, start={}, bookOpen={}",
	            req.getId(), req.getStart(), req.getBookOpen());

	    Concert concert = concertRepository.findById(req.getId())
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Concert ID입니다."));

	    Schedule schdule = Schedule.builder()
	            .concert(concert)
	            .start(req.getStart())
	            .bookOpen(req.getBookOpen())
	            .build();

	    Schedule newSchedule = scheduleRepository.save(schdule);

	    log.info("스케줄 생성 완료. scheduleId={}", newSchedule.getId());

	    SeatGrade grade;
	    Long price;
	    List<Seat> seatList = new ArrayList<>();

		for (int i = 1; i <= 30000; i++) {
			if (i <= 3000) {
				grade = SeatGrade.VIP;
				price = 150_000L;
			} else if (i >= 3001 && i <= 15000) {
				grade = SeatGrade.R;
				price = 120_000L;
			} else {
				grade = SeatGrade.S;
				price = 100_000L;
			}

	        Seat seat = Seat.builder()
	                .number(i + "번")
	                .grade(grade)
	                .price(price)
	                .schedule(newSchedule)
	                .build();

	        seatList.add(seat);
	    }

	    seatRepository.saveAll(seatList);

	    log.info("좌석 생성 완료. scheduleId={}, seatCount={}",
	            newSchedule.getId(), seatList.size());

	    return newSchedule.getId();
	}
}
