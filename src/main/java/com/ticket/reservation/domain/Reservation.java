package com.ticket.reservation.domain;

import java.time.LocalDateTime;

import com.ticket.schedule.domain.Schedule;
import com.ticket.seat.domain.Seat;
import com.ticket.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "r_id")
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "u_id")
	private User user;
	
	@ManyToOne
	@JoinColumn(name = "sc_id")
	private Schedule schedule;
	
	@ManyToOne
	@JoinColumn(name = "s_id")
	private Seat seat;
	
	@Enumerated(EnumType.STRING)
	@Column(name="r_status", nullable = false)
	private ReservationStatus status;
	
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
	public Reservation(User user, Schedule schedule, Seat seat, ReservationStatus status) {
		this.user = user;
		this.schedule = schedule;
		this.seat = seat;
		this.status = status;
	}
}
