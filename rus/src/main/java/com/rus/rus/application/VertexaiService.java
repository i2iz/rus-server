package com.rus.rus.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.FunctionResponse; // import 추가
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.Struct; // import 추가
import com.google.protobuf.Value; // import 추가
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto; // DTO 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로깅을 위해 추가
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map; // import 추가
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j // Log (동작 확인을 위해 추천)
@Service
@RequiredArgsConstructor
public class VertexaiService {

  private final GenerativeModel generativeModel;
  private final RoutineService routineService; // 실제 루틴 추가 로직
  private final ObjectMapper objectMapper; // (현재는 사용되지 않으나, 다른 함수 호출 시 필요할 수 있음)

  /**
   * VertexaiController로부터 호출될 메인 메소드
   * * @param uid JWT 토큰에서 추출된 사용자 ID
   *
   * @param messages 전체 대화 기록 (History)
   * @return AI의 최종 텍스트 응답
   * @throws IOException Vertex AI 통신 오류
   */
  public String getChatResponse(UUID uid, List<ChatMessageDto> messages) throws IOException {

    // 1. ChatSession 시작
    ChatSession chatSession = generativeModel.startChat();

    // 2. [오류 1 수정]
    // API DTO (messages) -> Vertex AI 형식 (Content)으로 변환
    // ChatSession은 내부에서 히스토리를 관리하므로, 먼저 히스토리를 설정(set)해야 합니다.
    List<Content> history = new ArrayList<>();
    if (messages.size() > 1) {
      history = messages.stream()
          .limit(messages.size() - 1) // 마지막 메시지 제외
          .map(this::convertDtoToContent)
          .collect(Collectors.toList());

      // ChatSession에 이전 대화 기록을 로드
      chatSession.setHistory(history);
    }

    // 3. 현재 사용자의 질문 (리스트의 가장 마지막 메시지)
    String currentUserMessage = messages.get(messages.size() - 1).getText();

    // 4. [오류 1 수정]
    // AI 모델에 '현재 메시지'만 전송 (히스토리는 ChatSession이 이미 알고 있음)
    log.info("VertexAI 요청 전송: {}", currentUserMessage);
    GenerateContentResponse response = chatSession.sendMessage(currentUserMessage);

    // 5. AI 응답 처리
    log.info("VertexAI 응답 수신");
    Part responsePart = response.getCandidates(0).getContent().getParts(0);

    // 6. AI가 Function Call을 요청했는지 확인
    if (responsePart.hasFunctionCall()) {
      FunctionCall functionCall = responsePart.getFunctionCall();
      log.info("Function Call 감지: {}", functionCall.getName());

      // 7. Config에서 정의한 'addCustomRoutine' 함수가 맞는지 확인
      if (functionCall.getName().equals("addCustomRoutine")) {

        // 8. [오류 2 수정]
        // AI가 전달한 Protobuf Struct 인자를 수동으로 파싱
        // (ResponseHandler.functionCallToMap은 존재하지 않음)
        Map<String, Value> args = functionCall.getArgs().getFieldsMap();

        String content;
        int categoryId;

        try {
          content = args.get("content").getStringValue();
          // getNumberValue()는 double을 반환하므로 int로 캐스팅
          categoryId = (int) args.get("categoryId").getNumberValue();
        } catch (Exception e) {
          log.error("FunctionCall 인자 파싱 실패: {}", e.getMessage());
          return "AI가 잘못된 함수 인자(content 또는 categoryId)를 전달했습니다.";
        }

        // DTO 객체 생성
        RoutineAddCustomRequestDto routineDto = new RoutineAddCustomRequestDto();
        routineDto.setContent(content);
        routineDto.setCategoryId(categoryId);

        // 9. 실제 Java 서비스 로직 호출 (DB에 저장)
        try {
          log.info("RoutineService.addCustomRoutineToUser 호출. uid: {}, content: {}, categoryId: {}",
              uid, routineDto.getContent(), routineDto.getCategoryId());

          routineService.addCustomRoutineToUser(uid.toString(), routineDto);

          // 10. 함수 실행 성공 결과를 AI에게 다시 전송
          Struct.Builder responseStruct = Struct.newBuilder();
          responseStruct.putFields("status", Value.newBuilder().setStringValue("SUCCESS").build());
          responseStruct.putFields("message", Value.newBuilder().setStringValue("루틴이 성공적으로 추가되었습니다.").build());

          // [오류 3 수정]
          // FunctionResponse를 Part로 감싸고, Part를 다시 Content로 감싸서 전송
          Part functionResponsePart = Part.newBuilder()
              .setFunctionResponse(
                  FunctionResponse.newBuilder()
                      .setName(functionCall.getName())
                      .setResponse(responseStruct.build())
                      .build())
              .build();

          Content functionResponseContent = Content.newBuilder().addParts(functionResponsePart).build();
          GenerateContentResponse finalResponse = chatSession.sendMessage(functionResponseContent);

          // 11. AI의 최종 텍스트 응답 반환
          return ResponseHandler.getText(finalResponse);

        } catch (Exception e) {
          log.error("루틴 추가 중 오류 발생: {}", e.getMessage());

          // 10-b. 함수 실행 '실패' 결과를 AI에게 전송
          Struct.Builder errorStruct = Struct.newBuilder();
          errorStruct.putFields("status", Value.newBuilder().setStringValue("ERROR").build());
          errorStruct.putFields("message",
              Value.newBuilder().setStringValue("루틴 추가에 실패했습니다: " + e.getMessage()).build());

          // [오류 3 수정]
          Part errorResponsePart = Part.newBuilder()
              .setFunctionResponse(
                  FunctionResponse.newBuilder()
                      .setName(functionCall.getName())
                      .setResponse(errorStruct.build())
                      .build())
              .build();

          Content errorResponseContent = Content.newBuilder().addParts(errorResponsePart).build();
          GenerateContentResponse errorResponse = chatSession.sendMessage(errorResponseContent);

          return ResponseHandler.getText(errorResponse);
        }
      } else {
        log.warn("알 수 없는 Function Call 요청: {}", functionCall.getName());
        return "AI가 알 수 없는 함수를 호출했습니다: " + functionCall.getName();
      }
    }

    // 12. Function Call이 아닌, 일반 텍스트 응답을 받은 경우
    log.info("일반 텍스트 응답 반환");
    return ResponseHandler.getText(response);
  }

  /**
   * ChatMessageDto (API DTO)를 Vertex AI의 Content 객체로 변환합니다.
   */
  private Content convertDtoToContent(ChatMessageDto dto) {
    String role;
    if (dto.getRole().equalsIgnoreCase("MODEL")) {
      role = "model";
    } else {
      role = "user"; // 기본값 USER
    }

    return Content.newBuilder()
        .setRole(role)
        .addParts(Part.newBuilder().setText(dto.getText()).build())
        .build();
  }
}