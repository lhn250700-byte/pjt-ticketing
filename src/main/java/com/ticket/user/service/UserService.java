package com.ticket.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ticket.user.domain.User;
import com.ticket.user.dto.UserResponse;
import com.ticket.user.dto.UserSignUpReqeust;
import com.ticket.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {
	private final UserRepository userRepository;
	
	@Transactional
	public Long signUp(UserSignUpReqeust req) {

	    log.info("회원가입 시작. email={}", req.getEmail());

	    User user = User.builder()
	                    .email(req.getEmail())
	                    .password(req.getPassword())
	                    .name(req.getName())
	                    .build();

	    User newUser = userRepository.save(user);

	    log.info("회원가입 완료. userId={}", newUser.getId());

	    return newUser.getId();
	}

	public UserResponse findUser(Long id) {

	    log.debug("유저 조회 시작. userId={}", id);

	    User user = userRepository.findById(id)
	            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

	    log.debug("유저 조회 완료. userId={}", id);

	    return UserResponse.from(user);
	}
}
