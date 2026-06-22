package com.ticket.payment.controller;

import com.ticket.payment.dto.PaymentRequest;
import com.ticket.payment.dto.PaymentResponse;
import com.ticket.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest req) throws BadRequestException {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.pay(req));
    }
}
