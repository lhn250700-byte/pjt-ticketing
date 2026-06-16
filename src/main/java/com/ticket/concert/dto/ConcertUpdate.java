package com.ticket.concert.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ConcertUpdate {
    private String title;
    private String description;
    private String venue;
    private Integer runtime;
}
