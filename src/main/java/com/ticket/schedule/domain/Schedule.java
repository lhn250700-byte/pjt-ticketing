package com.ticket.schedule.domain;

import java.time.LocalDateTime;

import com.ticket.concert.domain.Concert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "sc_id")
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "c_id")
	private Concert concert;
	
	@Column(name = "sc_start")
	private LocalDateTime start;
	
	@Column(name = "sc_book_open")
	private LocalDateTime bookOpen;
	
	private LocalDateTime createdAt;
	
	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
	
	@Builder
	public Schedule(Concert concert, LocalDateTime start, LocalDateTime bookOpen) {
		this.concert = concert;
		this.start = start;
		this.bookOpen = bookOpen;
	}
}
