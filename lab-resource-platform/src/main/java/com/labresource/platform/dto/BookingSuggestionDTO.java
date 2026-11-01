package com.labresource.platform.dto;

import java.time.LocalDateTime;

public class BookingSuggestionDTO {

    private LocalDateTime suggestedStartTime;
    private LocalDateTime suggestedEndTime;
    private String message;

    public BookingSuggestionDTO(
            LocalDateTime suggestedStartTime,
            LocalDateTime suggestedEndTime,
            String message
    ) {
        this.suggestedStartTime = suggestedStartTime;
        this.suggestedEndTime = suggestedEndTime;
        this.message = message;
    }

    public LocalDateTime getSuggestedStartTime() {
        return suggestedStartTime;
    }

    public LocalDateTime getSuggestedEndTime() {
        return suggestedEndTime;
    }

    public String getMessage() {
        return message;
    }
}