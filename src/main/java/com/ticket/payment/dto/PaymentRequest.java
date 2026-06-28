package com.ticket.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private Long userId;
    private Long scheduleId;
    private Long seatId;
    private Long reservationId;
    private Long amount;
    private String method;
}
