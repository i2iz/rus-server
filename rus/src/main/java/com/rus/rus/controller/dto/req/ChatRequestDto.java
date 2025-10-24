package com.rus.rus.controller.dto.req;

import com.rus.rus.controller.dto.ChatMessageDto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 챗봇 API 요청 DTO
 * 클라이언트는 사용자의 새 메시지를 포함한 *전체* 대화 기록을 전송합니다.
 */
@Data
@NoArgsConstructor
public class ChatRequestDto {

  /**
   * 전체 대화 기록 (History)
   * 예: [USER_MSG_1, MODEL_MSG_1, USER_MSG_2]
   */
  private List<ChatMessageDto> messages;
}