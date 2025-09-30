package com.rus.rus.controller.dto.res;


import com.rus.rus.controller.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponseDto {
    private List<ChatMessageDto> messages;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalMessages;
}