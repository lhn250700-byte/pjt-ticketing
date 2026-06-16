package com.ticket.schedule.dto;

import com.ticket.schedule.domain.Schedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleResponse {
    private Long scheduleId;
    private LocalDateTime bookOpenTime;
    private LocalDateTime startTime;

    @Builder
    private ScheduleResponse(Long scheduleId, LocalDateTime bookOpenTime, LocalDateTime startTime) {
        this.scheduleId = scheduleId;
        this.bookOpenTime = bookOpenTime;
        this.startTime = startTime;
    }

    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .scheduleId(schedule.getId())
                .bookOpenTime(schedule.getBookOpen())
                .startTime(schedule.getStart())
                .build();
    }
}
