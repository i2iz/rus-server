package com.rus.rus.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.FunctionResponse;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ChatSession;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.rus.rus.controller.dto.ChatMessageDto;
import com.rus.rus.controller.dto.req.RoutineAddCustomRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VertexaiService {

  private final GenerativeModel generativeModel;
  private final RoutineService routineService;
  private final ObjectMapper objectMapper; // 사용되지 않지만 유지

  public String getChatResponse(String uid, List<ChatMessageDto> messages) throws IOException {

    ChatSession chatSession = generativeModel.startChat();

    List<Content> history = new ArrayList<>();
    if (messages.size() > 1) {
      history = messages.stream()
          .limit(messages.size() - 1)
          .map(this::convertDtoToContent)
          .collect(Collectors.toList());
      chatSession.setHistory(history);
    }

    String currentUserMessage = messages.get(messages.size() - 1).getText();

    log.info("VertexAI 요청 전송: {}", currentUserMessage);
    GenerateContentResponse response = chatSession.sendMessage(currentUserMessage);
    log.info("VertexAI 응답 수신");

    // AI 응답의 *모든* Part를 확인
    List<Part> responseParts = response.getCandidates(0).getContent().getPartsList();
    List<Part> functionResponseParts = new ArrayList<>(); // Function Response들을 담을 리스트

    boolean hasFunctionCall = false;
    for (Part part : responseParts) {
      if (part.hasFunctionCall()) {
        hasFunctionCall = true;
        FunctionCall functionCall = part.getFunctionCall();
        log.info("Function Call 감지: {}", functionCall.getName());

        if (functionCall.getName().equals("addCustomRoutine")) {
          // Function Call 인자 파싱
          Map<String, Value> args = functionCall.getArgs().getFieldsMap();
          String content;
          int categoryId;
          try {
            content = args.get("content").getStringValue();
            categoryId = (int) args.get("categoryId").getNumberValue();
          } catch (Exception e) {
            log.error("FunctionCall 인자 파싱 실패: {}", e.getMessage());
            // 파싱 실패 시 에러 응답 Part 생성
            Struct.Builder errorStruct = Struct.newBuilder();
            errorStruct.putFields("status", Value.newBuilder().setStringValue("ERROR").build());
            errorStruct.putFields("message",
                Value.newBuilder().setStringValue("AI가 잘못된 함수 인자(content 또는 categoryId)를 전달했습니다.").build());
            functionResponseParts.add(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionCall.getName())
                    .setResponse(errorStruct.build())
                    .build())
                .build());
            continue; // 다음 Part 처리로 넘어감
          }

          // DTO 생성
          RoutineAddCustomRequestDto routineDto = new RoutineAddCustomRequestDto();
          routineDto.setContent(content);
          routineDto.setCategoryId(categoryId);

          // 실제 서비스 로직 호출
          try {
            log.info("RoutineService.addCustomRoutineToUser 호출. uid: {}, content: {}, categoryId: {}",
                uid, routineDto.getContent(), routineDto.getCategoryId());
            routineService.addCustomRoutineToUser(uid, routineDto);

            // 성공 응답 Part 생성 및 리스트에 추가
            Struct.Builder responseStruct = Struct.newBuilder();
            responseStruct.putFields("status", Value.newBuilder().setStringValue("SUCCESS").build());
            responseStruct.putFields("message",
                Value.newBuilder().setStringValue("루틴 '" + content + "'이(가) 성공적으로 추가되었습니다.").build());
            functionResponseParts.add(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionCall.getName())
                    .setResponse(responseStruct.build())
                    .build())
                .build());

          } catch (Exception e) {
            log.error("루틴 추가 중 오류 발생: {}", e.getMessage());
            // 서비스 로직 실패 시 에러 응답 Part 생성 및 리스트에 추가
            Struct.Builder errorStruct = Struct.newBuilder();
            errorStruct.putFields("status", Value.newBuilder().setStringValue("ERROR").build());
            errorStruct.putFields("message",
                Value.newBuilder().setStringValue("루틴 '" + content + "' 추가에 실패했습니다: " + e.getMessage()).build());
            functionResponseParts.add(Part.newBuilder()
                .setFunctionResponse(FunctionResponse.newBuilder()
                    .setName(functionCall.getName())
                    .setResponse(errorStruct.build())
                    .build())
                .build());
          }
        } else {
          // 알 수 없는 함수 호출 처리
          log.warn("알 수 없는 Function Call 요청: {}", functionCall.getName());
          Struct.Builder errorStruct = Struct.newBuilder();
          errorStruct.putFields("status", Value.newBuilder().setStringValue("UNKNOWN_FUNCTION").build());
          errorStruct.putFields("message",
              Value.newBuilder().setStringValue("알 수 없는 함수 '" + functionCall.getName() + "' 호출됨").build());
          functionResponseParts.add(Part.newBuilder()
              .setFunctionResponse(FunctionResponse.newBuilder()
                  .setName(functionCall.getName()) // 받은 이름 그대로 반환
                  .setResponse(errorStruct.build())
                  .build())
              .build());
        }
      }
      // Function Call이 아닌 다른 Part(예: 일반 텍스트)는 무시하고 다음 Part 확인
    }

    // Function Call이 하나라도 있었는지 확인
    if (hasFunctionCall) {
      // 수집된 모든 Function Response Part들을 담아서 AI에게 다시 전송
      Content functionResponsesContent = Content.newBuilder().addAllParts(functionResponseParts).build();
      log.info("{}개의 Function Response 전송", functionResponseParts.size());
      GenerateContentResponse finalResponse = chatSession.sendMessage(functionResponsesContent);
      // AI의 최종 텍스트 응답 반환
      return ResponseHandler.getText(finalResponse);
    } else {
      // Function Call이 없었다면, 원래 받은 응답에서 텍스트를 추출하여 반환
      log.info("일반 텍스트 응답 반환");
      return ResponseHandler.getText(response);
    }
  }

  private Content convertDtoToContent(ChatMessageDto dto) {
    String role;
    if (dto.getRole().equalsIgnoreCase("MODEL")) {
      role = "model";
    } else {
      role = "user";
    }
    return Content.newBuilder()
        .setRole(role)
        .addParts(Part.newBuilder().setText(dto.getText()).build())
        .build();
  }
}