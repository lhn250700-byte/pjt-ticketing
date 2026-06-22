package com.ticket.queue.controller;

import com.ticket.queue.dto.QueueResponse;
import com.ticket.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    // 선착순 대기열 진입 API
    @PostMapping("/join")
    public ResponseEntity<QueueResponse> joinQueue(
            @RequestParam("scheduleId") Long scheduleId,
            @RequestParam("userId") Long userId
    ) throws BadRequestException {
        QueueResponse response = queueService.registerQueue(scheduleId, userId);
        return ResponseEntity.ok(response);
    }
}
