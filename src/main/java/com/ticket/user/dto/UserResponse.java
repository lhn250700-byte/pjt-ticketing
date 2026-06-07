package com.ticket.user.dto;

import com.ticket.user.domain.User;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserResponse {
	private String email;
	private String name;
	
	@Builder
	private UserResponse(String email, String name) {
		this.email = email;
		this.name = name;
	}
	
	public static UserResponse from(User user) {
		return UserResponse.builder()
				.email(user.getEmail())
				.name(user.getName())
				.build();
	}
}
