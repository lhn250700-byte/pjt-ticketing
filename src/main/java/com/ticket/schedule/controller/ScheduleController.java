package com.ticket.schedule.controller;

import com.ticket.schedule.dto.ScheduleResponse;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ticket.schedule.dto.ScheduleCreateRequest;
import com.ticket.schedule.service.ScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
@Slf4j
public class ScheduleController {
	private final ScheduleService scheduleService;
	
	@PostMapping
	public ResponseEntity<Long> createSchedule(@Valid @RequestBody ScheduleCreateRequest req) {

	    log.info("스케줄 생성 요청. concertId={}, bookOpen={}, start={}",
	            req.getId(), req.getBookOpen(), req.getStart());

	    Long scheduleId = scheduleService.createSchedule(req);

	    log.info("스케줄 생성 완료. scheduleId={}", scheduleId);

	    return ResponseEntity.status(HttpStatus.CREATED).body(scheduleId);
	}

	// 단건 조회
	@GetMapping("/{scheduleId}")
	public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable Long scheduleId) {
		return ResponseEntity.ok(scheduleService.getSchedule(scheduleId));
	}

	// 삭제
	@DeleteMapping("/{scheduleId}")
	public ResponseEntity<String> deleteSchedule(@PathVariable Long scheduleId) {
		scheduleService.deleteSchedule(scheduleId);
		return ResponseEntity.ok(scheduleId + " 삭제 완료");
	}
}
