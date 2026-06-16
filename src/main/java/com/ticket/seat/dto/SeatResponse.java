package com.ticket.seat.dto;

import com.ticket.seat.domain.Seat;
import com.ticket.seat.domain.SeatGrade;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SeatResponse {
    private Long id;
    private SeatGrade grade;
    private String number;
    private Long price;
    private Boolean isReserved;

    @Builder
    private SeatResponse(Long id, SeatGrade grade, String number, Long price, Boolean isReserved) {
        this.id = id;
        this.grade = grade;
        this.number = number;
        this.price = price;
        this.isReserved = isReserved;
    }

    public static SeatResponse from (Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .grade(seat.getGrade())
                .number(seat.getNumber())
                .price(seat.getPrice())
                .isReserved(seat.getIsReserved())
                .build();
    }
}
