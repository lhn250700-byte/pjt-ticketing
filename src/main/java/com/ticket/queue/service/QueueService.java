package com.ticket.queue.service;

import com.ticket.queue.dto.QueueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final StringRedisTemplate redisTemplate;

    // 대기열 진입 및 번호표 (토큰) 발급
    public QueueResponse registerQueue(Long scheduleId, Long userId) {
        // 회차별로 대기열 key 생성 ex) concert:queue:1
        String queueKey = "concert:queue:" + scheduleId;
        String userTokenKey = "concert:user:" + userId + ":schedule:" + scheduleId;

        // 해당 유저가 기발급 토큰이 있는지 조회
        String existingToken = redisTemplate.opsForValue().get(userTokenKey);

        // 기발급 토큰 있는 경우, 기존 토큰으로 순번만 조회하여 반환
        if (existingToken != null) {
            String queueValue = existingToken + ":" + userId;
            Long rank = redisTemplate.opsForZSet().rank(queueKey, queueValue);

            return QueueResponse.builder()
                    .token(existingToken)
                    .rank(rank)
                    .build();
        }

        // 고유 uuid 토큰 생성 및 토큰:유저id 조합으로 value 정의
        String token = UUID.randomUUID().toString();
        String queueValue = token + ":" + userId;

        // 현재 시간을 Score로 지정하여 선착순 정렬 기준 마련
        double score = System.currentTimeMillis();

        // Redis Sorted Seet에 추가 (zadd)
        // zadd queueKey score queueValue
        redisTemplate.opsForZSet().add(queueKey, queueValue, score);


        // 유효기간 10분
        redisTemplate.opsForValue().set(userTokenKey, token, Duration.ofMinutes(10));

        // 방금 추가된 유저의 대기 순번 조회 (zrank)
        Long rank = redisTemplate.opsForZSet().rank(queueKey, queueValue);

        return QueueResponse.builder()
                .token(token)
                .rank(rank)
                .build();
    }
}
