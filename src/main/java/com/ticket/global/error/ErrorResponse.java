package com.ticket.global.error;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
	private final String error; // 에러 이름
	private final String message; // 에러 메시지
	private final int status; // HTTP 상태 코드
}
