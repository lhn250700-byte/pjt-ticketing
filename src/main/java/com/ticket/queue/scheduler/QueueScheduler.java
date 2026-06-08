package com.ticket.queue.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final StringRedisTemplate redisTemplate;

    // 1초마다 주기적으로 대기열의 상위 유저들을 활성화 상태로 전환
    @Scheduled(fixedDelay = 1000)
    public void moveUserFromWaitToActive() {
        Long scheduleId = 1L; // 모든 스케줄 일정을 돌리기 위해 동적 처리해야 하지만, 테스트를 위해 고정

        String waitKey = "concert:queue:" + scheduleId;
        String activeKey = "concert:queue:active:" + scheduleId;

        // 1초에 몇 명씩 통과시킬지 설정 (놀이공원 방식) (= 서버가 감당 가능한 수치)
        long enterCount = 10L;

        // Wait 대기열에서 가장 오래 기다린 상위 10명 추출
        Set<String> waitUsers = redisTemplate.opsForZSet().range(waitKey, 0, enterCount - 1);

        if (waitUsers == null || waitUsers.isEmpty()) return; // 대기 중인 유저가 없으면 스케줄러 종료

        log.info("[스케줄러 작동] 대기열 유저 {}명을 활성화 방(Active)으로 진입시킵니다.", waitUsers.size());

        for (String userValue : waitUsers) {
            // 통과된 유저들 Active 방에 넣기
            redisTemplate.opsForSet().add(activeKey, userValue);

            // 통과 후 기존 대기열 삭제
            redisTemplate.opsForZSet().remove(waitKey, userValue);
        }
    }
}
