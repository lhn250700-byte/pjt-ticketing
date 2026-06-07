package com.ticket.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticket.user.dto.UserResponse;
import com.ticket.user.dto.UserSignUpReqeust;
import com.ticket.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class UserController {
	private final UserService userService;
	
	@PostMapping
	public ResponseEntity<Long> signUp(@Valid @RequestBody UserSignUpReqeust req) {

	    log.info("회원가입 요청. email={}", req.getEmail());

	    Long userId = userService.signUp(req);

	    log.info("회원가입 완료. userId={}", userId);

	    return ResponseEntity.status(HttpStatus.CREATED).body(userId);
	}

	@GetMapping("/{id}")
	public ResponseEntity<UserResponse> findUser(@PathVariable Long id) {

	    log.info("유저 조회 요청. userId={}", id);

	    UserResponse response = userService.findUser(id);

	    log.info("유저 조회 완료. userId={}", id);

	    return ResponseEntity.ok(response);
	}
}
