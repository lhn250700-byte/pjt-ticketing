package com.ticket.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class LuaScriptConfig {
//    Lua 스크립트 정의
//    return value
//    -1: 이미 쿠폰을 발급받은 사용자
//     0: 쿠폰 재고 소진
//     1: 쿠폰 발급 성공
//     KEYS[1]: 잔여 좌석 수량 키 (예: concert:schedule:1:available_seats)
//     KEYS[2]: 유저의 선점 기록 키 (예: concert:schedule:1:user:456:hold)
//     ARGV[1]: 요청 좌석 수량 (예: 1 또는 2)
//     ARGV[2]: 임시 선점 만료 시간 (초 단위, 예: 300)
    @Bean
    public RedisScript<Long> holdSeatScript() {
        String script =
                // 1. 해당 좌석이 이미 선점되어 존재하는지 확인 (이미 있으면 0 리턴하며 즉시 튕겨냄)
                "if redis.call('exists', KEYS[1]) == 1 then " +
                        "return 0 " +
                        "end " +

                        // 2. 유저가 이미 다른 좌석을 선점하고 있는지 확인 (중복 선점 방지)
                        "if redis.call('exists', KEYS[2]) == 1 then " +
                        "return -2 " +
                        "end " +

                        // 3. 전체 잔여 재고 키 검증
                        "local available = redis.call('get', KEYS[3]) " +
                        "if not available then return -1 end " +
                        "available = tonumber(available) " +
                        "local requested = tonumber(ARGV[1]) " + // 보통 1개 요청

                        // 4. 재고가 남아있다면 원자적으로 좌석 점유 및 수량 차감 진행
                        "if available >= requested then " +
                        "redis.call('decrby', KEYS[3], requested) " +                          // 전체 재고 차감
                        "redis.call('set', KEYS[1], ARGV[3], 'EX', ARGV[2]) " +                // 좌석 고유 키 생성 (값: userId, TTL: 5분)
                        "redis.call('set', KEYS[2], ARGV[3], 'EX', ARGV[2]) " +                // 유저 hold 키 생성 (값: userId, TTL: 5분)
                        "return 1 " + // 최초 성공자 딱 1명만 1을 받음
                        "else " +
                        "return -3 " + // 전체 재고 부족
                        "end";

        return new DefaultRedisScript<>(script, Long.class);
    }
}
