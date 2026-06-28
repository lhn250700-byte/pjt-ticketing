package com.ticket.concert.domain;

import com.ticket.global.error.BusinessException;
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
public class Concert {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "c_id")
	private Long id;
	
	@Column(name = "c_title", length = 20, nullable = false)
	private String title;
	
	@Column(name = "c_desc", length = 100)
	private String description;

	@Column(name = "c_venue", length = 100, nullable = false)
	private String venue;
	
	@Column(name = "c_runtime", nullable = false)
	private Integer runtime;

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
	public Concert(String title, String description, String venue, Integer runtime) {
		this.title = title;
		this.runtime = runtime;
		this.venue = venue;
		this.description = description;
	}

	public void update(String title, String description, String venue, Integer runtime) {
		if (title != null) {
			if (title.length() > 20) throw new BusinessException(HttpStatus.BAD_REQUEST, "공연 제목은 최대 20글자 제한입니다.");
			this.title = title;
		}
		if (description != null) {
			if (description.length() > 100) throw new BusinessException(HttpStatus.BAD_REQUEST, "공연 설명은 최대 100글자 제한입니다.");
			this.description = description;
		}
		if (venue != null) {
			if (venue.length() > 100) throw new BusinessException(HttpStatus.BAD_REQUEST, "공연 장소는 최대 100글자 제한입니다.");
			this.venue = venue;
		}
		if (runtime != null) {
			if (runtime < 1) throw new BusinessException(HttpStatus.BAD_REQUEST, "공연 시간은 1분 이상이어야 합니다.");
			this.runtime = runtime;
		}
	}
}
