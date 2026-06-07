package com.ticket.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserSignUpReqeust {
	@Email
	@NotBlank
	private String email;
	@NotBlank
	private String password;
	@NotBlank
	@Size(max = 10)
	private String name;
}
