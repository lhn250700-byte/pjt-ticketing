package com.ticket.payment.domain;

import com.ticket.reservation.domain.Reservation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "p_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "r_id", nullable = false)
    private Reservation reservation;

    @Column(name = "p_amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "p_status")
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "p_method")
    private PaymentMethod method;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Payment(Reservation reservation, Long amount, PaymentStatus status, PaymentMethod method) {
        this.reservation = reservation;
        this.amount = amount;
        this.status = status;
        this.method = method;
    }
}
