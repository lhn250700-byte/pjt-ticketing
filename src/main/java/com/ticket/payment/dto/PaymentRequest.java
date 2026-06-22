package com.ticket.payment.dto;

import lombok.Getter;

@Getter
public class PaymentRequest {
    private Long userId;
    private Long scheduleId;
    private Long seatId;
    private Long amount;
    private String method;
}
