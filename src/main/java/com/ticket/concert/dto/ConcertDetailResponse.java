package com.ticket.concert.dto;

import com.ticket.concert.domain.Concert;
import com.ticket.schedule.dto.ScheduleResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class ConcertDetailResponse {
    private Long concertId;
    private String title;
    private String description;
    private String venue;
    private Integer runtimeMinutes;
    private List<ScheduleResponse> schedules;

    @Builder
    private ConcertDetailResponse(Long concertId, String title, String description, String venue, Integer runtimeMinutes, List<ScheduleResponse> schedules) {
        this.concertId = concertId;
        this.title = title;
        this.description = description;
        this.venue = venue;
        this.runtimeMinutes = runtimeMinutes;
        this.schedules = schedules;
    }

    public static ConcertDetailResponse from(Concert concert, List<ScheduleResponse> schedules) {
        return ConcertDetailResponse.builder()
                .concertId(concert.getId())
                .title(concert.getTitle())
                .description(concert.getDescription())
                .venue(concert.getVenue())
                .runtimeMinutes(concert.getRuntime())
                .schedules(schedules)
                .build();
    }
}
