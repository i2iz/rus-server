package com.rus.rus.application;

import com.fasterxml.jackson.databind.ObjectMapper; // FunctionCall 파싱에 필요
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.rus.rus.controller.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VertexaiService {

  private final GenerativeModel generativeModel;
  private final RoutineService routineService;
  private final ObjectMapper objectMapper;

  /**
   * VertexaiController로부터 호출될 메인 메소드
   * * @param uid JWT 토큰에서 추출된 사용자 ID
   * 
   * @param messages 전체 대화 기록 (History)
   * @return AI의 최종 텍스트 응답
   * @throws IOException Vertex AI 통신 오류
   */
  public String getChatResponse(String uid, List<ChatMessageDto> messages) throws IOException {

    // --- (여기에 AI 호출 및 함수 처리 로직이 구현될 예정) ---

    // 현재는 placeholder 텍스트만 반환합니다.
    return "AI 응답 (구현 예정)";
  }

}