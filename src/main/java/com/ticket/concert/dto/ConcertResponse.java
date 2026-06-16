package com.ticket.concert.dto;

import com.ticket.concert.domain.Concert;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ConcertResponse {
	private Long id;
	private String title;
	private String description;
	private String venue;
	private Integer runtime;

	@Builder
	private ConcertResponse(Long id, String title, String description, String venue, Integer runtime) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.venue = venue;
		this.runtime = runtime;
	}
	
	public static ConcertResponse from (Concert concert) {
		return ConcertResponse.builder()
				.id(concert.getId())
				.title(concert.getTitle())
				.description(concert.getDescription())
				.venue(concert.getVenue())
				.runtime(concert.getRuntime())
				.build();
	}
}
