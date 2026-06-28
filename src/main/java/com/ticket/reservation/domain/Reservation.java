package com.ticket.reservation.domain;

import com.ticket.schedule.domain.Schedule;
import com.ticket.seat.domain.Seat;
import com.ticket.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "r_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "u_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sc_id")
	private Schedule schedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "s_id")
	private Seat seat;
	
	@Enumerated(EnumType.STRING)
	@Column(name="r_status", nullable = false)
	private ReservationStatus status;
	
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void cancel() {
		this.status = ReservationStatus.CANCELED;
	}

	public void reserve() {
		this.status = ReservationStatus.CONFIRMED;
	}

	public void fail() {
		this.status = ReservationStatus.FAILED;
	}
	
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
	public Reservation(User user, Schedule schedule, Seat seat, ReservationStatus status) {
		this.user = user;
		this.schedule = schedule;
		this.seat = seat;
		this.status = status;
	}
}
