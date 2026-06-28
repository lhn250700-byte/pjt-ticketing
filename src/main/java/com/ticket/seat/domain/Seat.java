package com.ticket.seat.domain;

import com.ticket.global.error.BusinessException;
import com.ticket.schedule.domain.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "s_id")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
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
	
	public void reserve() {
		if (isReserved) throw new BusinessException(HttpStatus.CONFLICT, "이미 예약된 좌석입니다.");
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
