// src/main/java/com/rus/rus/controller/OpenAIController.java
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name="openai.enabled", havingValue="true", matchIfMissing=true)
@RequestMapping("/v1/openai")
public class OpenAIController {

    private final OpenAIService openAIService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponseDto> handleChatMessage(
            @RequestBody ChatRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID uid = UUID.fromString(userDetails.getUsername());
        String aiResponseText = openAIService.getChatResponse(uid.toString(), requestDto.getMessages());

        ChatResponseDto response =
                new ChatResponseDto(new ChatMessageDto("MODEL", aiResponseText), null);
        return ResponseEntity.ok(response);
    }
}
