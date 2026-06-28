package com.ticket.reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class MakeReservationRequest {
	@NotNull
	private Long seatId;
	@NotNull
	private Long userId;
	@NotNull
	private Long scheduleId;
	@NotNull
	private String queueToken;

	private Long amount;
	private String method;
}
