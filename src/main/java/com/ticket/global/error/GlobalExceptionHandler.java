package com.ticket.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	// 비즈니스 로직 예외 처리
	@ExceptionHandler(BusinessException.class)
	protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		log.warn("BusinessException 발생 - Status: {}, Message: {}", e.getStatus(), e.getMessage());
		
		ErrorResponse response = ErrorResponse.builder()
				.status(e.getStatus().value())
				.error(e.getStatus().name())
				.message(e.getMessage())
				.build();
		
		return ResponseEntity.status(e.getStatus()).body(response);
	}
	
	// 예측하지 못한 최상위 시스템 에러 처리
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("예상치 못한 시스템 에러 발생: ", e);
		
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ErrorResponse response = ErrorResponse.builder()
				.status(status.value())
				.error(status.name())
				.message("서버 내부에 에러가 발생했습니다.")
				.build();
				
		return ResponseEntity.status(status).body(response);
	}
}
