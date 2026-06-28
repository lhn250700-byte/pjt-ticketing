package com.ticket.schedule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.concert.domain.Concert;
import com.ticket.concert.repository.ConcertRepository;
import com.ticket.global.error.BusinessException;
import com.ticket.schedule.domain.Schedule;
import com.ticket.schedule.dto.ScheduleCreateRequest;
import com.ticket.schedule.dto.ScheduleResponse;
import com.ticket.schedule.repository.ScheduleRepository;
import com.ticket.seat.domain.Seat;
import com.ticket.seat.domain.SeatGrade;
import com.ticket.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScheduleService {
	private final ScheduleRepository scheduleRepository;
	private final ConcertRepository concertRepository;
	private final SeatRepository seatRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Transactional
	public Long createSchedule(ScheduleCreateRequest req) {

		log.info("스케줄 생성 시작. concertId={}, start={}, bookOpen={}", req.getId(), req.getStart(), req.getBookOpen());

		Concert concert = concertRepository.findById(req.getId())
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 Concert ID입니다."));

		Schedule schedule = Schedule.builder().concert(concert).start(req.getStart()).bookOpen(req.getBookOpen())
				.build();

		Schedule newSchedule = scheduleRepository.save(schedule);

		log.info("스케줄 생성 완료. scheduleId={}", newSchedule.getId());

		SeatGrade grade;
		Long price;
		List<Seat> seatList = new ArrayList<>();

		for (int i = 1; i <= 10000; i++) {
			if (i <= 1000) {
				grade = SeatGrade.VIP;
				price = 150_000L;
			} else if (i <= 5000) {
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

		log.info("좌석 생성 완료. scheduleId={}, seatCount={}", newSchedule.getId(), seatList.size());

		String inventoryKey = "concert:schedule:" + newSchedule.getId() + ":seat:count";
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime performanceStart = newSchedule.getStart();
		Duration ttl = Duration.between(now, performanceStart).plusDays(1);
		redisTemplate.opsForValue().set(inventoryKey, "30000");
		redisTemplate.expire(inventoryKey, ttl);

		log.info("[Redis] 스케줄 {}의 잔여 좌석 키가 생성되었습니다. 수량: {}", newSchedule.getId(), 30000);

		return newSchedule.getId();
	}

	// GET /schedules/{scheduleId}
	public ScheduleResponse getSchedule(Long scheduleId) {
		String scheduleKey = "schedule:" + scheduleId;
		String cachedData = redisTemplate.opsForValue().get(scheduleKey);

		if (cachedData != null) {
			try {
				return objectMapper.readValue(cachedData, ScheduleResponse.class);
			} catch (JsonProcessingException e) {
				log.error("[Schedule] 캐시 데이터 가져오기 실패", e);
				redisTemplate.delete(scheduleKey);
			}
		}

		Schedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."));
		ScheduleResponse response = ScheduleResponse.from(schedule);

		try {
			redisTemplate.opsForValue().set(scheduleKey, objectMapper.writeValueAsString(response),
					Duration.ofMinutes(10));
		} catch (JsonProcessingException e) {
			log.error("[Schedule] 캐싱 실패", e);
		}

		return response;
	}

	// DEL /schedules/{scheduleId}
	@Transactional
	public void deleteSchedule(Long scheduleId) {
		String scheduleKey = "schedule:" + scheduleId;
		String cachedData = redisTemplate.opsForValue().get(scheduleKey);
		Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."));
		scheduleRepository.delete(schedule);
		if (cachedData != null) redisTemplate.delete(scheduleKey);
	}
}
