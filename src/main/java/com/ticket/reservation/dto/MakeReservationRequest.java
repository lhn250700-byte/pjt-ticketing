package com.ticket.reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MakeReservationRequest {
	@NotNull
	private Long userId;
	@NotNull
	private Long scheduleId;
	@NotNull
	private Long seatId;
}
