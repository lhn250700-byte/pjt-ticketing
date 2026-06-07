package com.ticket.schedule.dto;

import java.time.LocalDateTime;

import com.ticket.concert.domain.Concert;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ScheduleCreateRequest {
	@NotNull
	private Long id;
	@NotNull
	private LocalDateTime start;
	@NotNull
	private LocalDateTime bookOpen;
	
}
