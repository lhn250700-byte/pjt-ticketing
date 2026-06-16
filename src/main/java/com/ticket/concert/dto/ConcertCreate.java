package com.ticket.concert.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ConcertCreate {
	@NotBlank
	@Size(max = 20, message = "공연 제목은 최대 20글자 제한입니다.")
	private String title;
	
	@Size(max = 100, message = "공연 설명은 최대 100글자 제한입니다.")
	private String description;

	@NotBlank
	@Size(max = 100, message = "공연 장소는 최대 100글자 제한입니다.")
	private String venue;
	
	@NotNull
	@Min(value = 1, message = "공연 시간은 1분 이상이어야 합니다.")
	private Integer runtime;
}
