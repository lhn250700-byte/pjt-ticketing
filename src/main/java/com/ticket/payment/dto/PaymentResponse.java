package com.ticket.payment.dto;

import com.ticket.payment.domain.Payment;
import com.ticket.payment.domain.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PaymentResponse {
    private Long id;
    private PaymentStatus status;

    @Builder
    private PaymentResponse(Long id, PaymentStatus status) {
        this.id = id;
        this.status = status;
    }

    public static PaymentResponse from (Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .build();
    }
}
