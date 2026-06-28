package com.ticket.concert.dto;

import lombok.Getter;

@Getter
public class ConcertUpdate {
    private String title;
    private String description;
    private String venue;
    private Integer runtime;
}
