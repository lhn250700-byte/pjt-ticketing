package com.ticket.queue.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class QueueResponse {
    private final String token; // 유저 식별용 UUID 토큰
    private final Long rank; // 현재 내 앞에 몇 명 대기중인지 (0부터 시작)

    @Builder
    public QueueResponse(String token, Long rank) {
        this.token = token;
        this.rank = rank;
    }
}
