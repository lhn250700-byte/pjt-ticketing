package com.ticket.queue.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 요청 헤더에서 토큰, userId, scheduleId를 꺼냄
        String token = request.getHeader("Queue-Token");
        String userId = request.getHeader("User-Id");
        String scheduleId = request.getParameter("scheduleId");

        // 필수 값이 누락된 경우 즉시 차단
        if (token == null || userId == null || scheduleId == null) {
            log.warn("[인터셉터 차단] 필수 대기열 인증 정보가 누락되었습니다.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "대기열 토큰 정보가 필요합니다.");
            return false;
        }

        String activeKey = "concert:queue:active:"+ scheduleId;
        String queueValue = token + ":" + userId;

        // Redis Active 방에 해당 유저가 존재하는지 확인
        Boolean isActive = redisTemplate.opsForSet().isMember(activeKey, queueValue);

        if (Boolean.FALSE.equals(isActive)) {
            log.warn("[인터셉터 차단] 비정상적인 접근입니다. 유저 {} (토큰: {})는 아직 Active 상태가 아닙니다.", userId, token);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "아직 예매 진입 순서가 아닙니다. 대기해주세요.");
            return false; // 컨트롤러로 요청 넘기지 않고 여기서 종료
        }

        log.info("[인터셉터 통과] 유저 {} 예매 API 진입 허가", userId);
        return true;
    }
}
