package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 챗봇과 주고받는 단일 메시지를 나타내는 DTO
 * (Request와 Response에서 공통으로 사용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

  /**
   * 메시지를 보낸 주체
   * "USER" 또는 "MODEL"
   */
  private String role;

  /**
   * 메시지 텍스트 내용
   */
  private String text;
}