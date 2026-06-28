package com.ticket.reservation.dto;

import com.ticket.payment.domain.PaymentStatus;
import lombok.Getter;

@Getter
public class ReservationRequestBody {
    private Long seatId;
    private Long amount;
    private String method;
}
