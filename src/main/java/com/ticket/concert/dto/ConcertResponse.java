package com.ticket.concert.dto;

import com.ticket.concert.domain.Concert;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ConcertResponse {
	private Long id;
	private String title;
	private String description;
	private Integer runtime;

	@Builder
	private ConcertResponse(Long id, String title, String description, Integer runtime) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.runtime = runtime;
	}
	
	public static ConcertResponse from (Concert concert) {
		return ConcertResponse.builder()
				.id(concert.getId())
				.title(concert.getTitle())
				.description(concert.getDescription())
				.runtime(concert.getRuntime())
				.build();
	}
}
