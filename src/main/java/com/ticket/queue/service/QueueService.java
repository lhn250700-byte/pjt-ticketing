package com.ticket.queue.service;

import com.ticket.global.error.BusinessException;
import com.ticket.queue.dto.QueueResponse;
import com.ticket.schedule.domain.Schedule;
import com.ticket.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {
    private final StringRedisTemplate redisTemplate;
    private final ScheduleRepository scheduleRepository;

    // 대기열 진입 및 번호표 (토큰) 발급
    @Transactional
    public QueueResponse registerQueue(Long scheduleId, Long userId) {
        // 회차별로 대기열 key 생성 ex) concert:queue:1
        String queueKey = "concert:queue:" + scheduleId; // Sorted Set 대기실
        String activeKey  = "concert:queue:active:" + scheduleId; // 활성화방 (Set)
        String userTokenKey = "concert:user:" + userId + ":schedule:" + scheduleId;
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."));
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(schedule.getBookOpen())) {
	        log.warn("예약 실패 - 티켓 오픈 전. scheduleId={}, bookOpen={}",
					scheduleId, schedule.getBookOpen());
	        throw new BusinessException(HttpStatus.BAD_REQUEST, "아직 티케팅 오픈 시간이 아닙니다.");
	    }

	    if (now.isAfter(schedule.getStart())) {
	        log.warn("예약 실패 - 공연 시작 이후. scheduleId={}, startTime={}",
					scheduleId, schedule.getStart());
	        throw new BusinessException(HttpStatus.GONE, "이미 시작했거나 종료된 공연입니다.");
	    }

        // 해당 유저가 기발급 토큰이 있는지 조회
        String existingToken = redisTemplate.opsForValue().get(userTokenKey);
        log.info("[대기열 진입 시도] 유저 ID: {}, 기발급 토큰 존재 여부: {}", userId, existingToken);

        // 기발급 토큰 있는 경우, 기존 토큰으로 순번만 조회하여 반환
        if (existingToken != null) {
            String queueValue = existingToken + ":" + userId;
            Long rank = redisTemplate.opsForZSet().rank(queueKey, queueValue);
            log.info("[대기열 재조회] 유저 ID: {}, 대기실 순번(ZRANK): {}", userId, rank);

            if (rank == null) {
                Boolean isActive = redisTemplate.opsForSet().isMember(activeKey, queueValue);

                if (Boolean.TRUE.equals(isActive)) {
                    log.info("유저 {}는 이미 대기열을 통과하여 활성화 상태입니다.", userId);
                    return QueueResponse.builder()
                            .token(existingToken)
                            .rank(0L)
                            .build();
                }

                log.info("유저 {}는 이미 최종 예매 처리가 완료되어 대기열 정보가 정리된 상태입니다.", userId);
                return QueueResponse.builder()
                        .token(existingToken)
                        .rank(-1L)
                        .build();
            }

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

        // Redis Sorted Set에 추가 (zadd)
        // zadd queueKey score queueValue
        redisTemplate.opsForZSet().add(queueKey, queueValue, score);

        // SET userTokenKey token EX 600
        redisTemplate.opsForValue().set(userTokenKey, token);

        // 방금 추가된 유저의 대기 순번 조회 (zrank)
        // ZRANK queueKey queueValue
        Long rank = redisTemplate.opsForZSet().rank(queueKey, queueValue);
        log.info("신규 유저 대기열 진입 완료 | 유저 {}, 발급 토큰 {} 대기 순번 {}", userId, token, rank);

        return QueueResponse.builder()
                .token(token)
                .rank(rank)
                .build();
    }
}
