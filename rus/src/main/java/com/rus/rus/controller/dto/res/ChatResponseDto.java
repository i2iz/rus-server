package com.rus.rus.controller.dto.res;

import com.rus.rus.controller.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇 API 응답 DTO
 * AI가 생성한 최종 텍스트 응답을 반환합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {

  /**
   * AI 모델이 생성한 응답 메시지 객체
   * (role은 "MODEL"이 됩니다)
   */
  private ChatMessageDto message;
}