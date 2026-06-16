package com.ticket.concert.dto;

import java.util.List;

public record CursorResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {
}
