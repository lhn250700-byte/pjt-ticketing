package com.ticket.seat.domain;

import java.time.LocalDateTime;

import com.ticket.schedule.domain.Schedule;

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
public class Seat {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "s_id")
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "sc_id")
	private Schedule schedule;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "s_grade", nullable = false, length = 20)
	private SeatGrade grade; // enum으로 수정하기
	
	@Column(name = "s_number", nullable = false, length = 20)	
	private String number;
	
	@Column(name = "s_price", nullable = false)
	private Long price;
	
	@Column(name = "s_is_reserved", nullable = false)
	private Boolean isReserved;
	
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
	
	public void reservation() {
		this.isReserved = true;
	}
	
	@Builder
	public Seat(SeatGrade grade, String number, Long price, Schedule schedule) {
		this.grade = grade;
		this.number = number;
		this.price = price;
		this.isReserved = false;
		this.schedule = schedule;
	}
}
