package com.rus.rus.controller.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDto {
    private String response;
    private String sessionId;
    private LocalDateTime timestamp;
    private String messageType;
    private Integer tokenCount;
}