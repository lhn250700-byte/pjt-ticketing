package com.ticket.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleCreateRequest {
	@NotNull
	private Long id;
	@NotNull
	private LocalDateTime start;
	@NotNull
	private LocalDateTime bookOpen;
	
}
