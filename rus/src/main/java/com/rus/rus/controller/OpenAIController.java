package com.rus.rus.controller;

import com.rus.rus.application.OpenAIService;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.ChatRequestDto;
import com.rus.rus.controller.dto.res.ChatResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/openai")
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true")
public class OpenAIController {

    private final OpenAIService openAIService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponseDto> handleChatMessage(
            @RequestBody ChatRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        UUID uid = UUID.fromString(userDetails.getUsername());
        String aiText = openAIService.getChatResponse(uid.toString(), requestDto.getMessages());

        ChatResponseDto resp = new ChatResponseDto(new ChatMessageDto("MODEL", aiText));
        return ResponseEntity.ok(resp);
    }
}

