package com.rus.rus.config;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.Type;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Collections;

@Configuration
public class VertexAiConfig {

  // application.properties 에서 값 주입
  @Value("${gcp.project.id}")
  private String projectId;

  @Value("${gcp.project.location}")
  private String location;

  @Value("${gcp.gemini.model.name}")
  private String modelName;

  /**
   * VertexAI 클라이언트 객체를 Spring Bean으로 등록합니다.
   * 이 객체가 생성될 때 SDK가 자동으로 GOOGLE_APPLICATION_CREDENTIALS 를 찾습니다.
   * 
   * @return VertexAI 싱글톤 빈
   * @throws IOException
   */
  @Bean
  public VertexAI vertexAI() throws IOException {
    // 별도로 키 파일을 명시하지 않아도 ADC에 의해 자동으로 인증됩니다.
    return new VertexAI(projectId, location);
  }

  /**
   * AI가 호출할 루틴 등록 함수(Tool)를 정의합니다.
   * 
   * @return Tool SingleTone Bean
   */
  @Bean
  public Tool functionCallingTool() {
    /// 이 안에 함수를 정의

    // 1. 루틴 직접 추가 : API 4-2
    FunctionDeclaration addCustomRoutine = FunctionDeclaration.newBuilder()
        .setName("addCustomRoutine") // VertexaiService에서 사용할 함수 이름
        .setDescription("사용자가 직접 입력한 텍스트 내용과 카테고리 ID로 새로운 개인 루틴을 추가합니다.")
        .setParameters(
            Schema.newBuilder()
                .setType(Type.OBJECT)
                // 1. 'content' 파라미터 (타입: STRING)
                .putProperties("content", Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("새로 추가할 루틴의 텍스트 내용입니다. (예: '아침 8시에 물 마시기')")
                    .build())
                // 2. 'categoryId' 파라미터 (타입: NUMBER)
                .putProperties("categoryId", Schema.newBuilder()
                    .setType(Type.NUMBER) // RoutineAddCustomRequestDto의 Integer에 해당
                    .setDescription(
                        "루틴이 속할 카테고리의 고유 ID입니다. " +
                            "사용자가 '운동' 카테고리라고 말하면 2를 사용해야 합니다. " +
                            "필요한 경우, 카테고리를 직접 추측합니다. " +
                            "다음은 실제 DB의 카테고리 이름과 ID 매핑입니다: " +
                            "'수면': 1, " +
                            "'운동': 2, " +
                            "'영양소': 3, " +
                            "'햇빛': 4, " +
                            "'사회적유대감': 5")
                    .build())
                // AI가 이 두 값을 반드시 함께 제공하도록 강제
                .addRequired("content")
                .addRequired("categoryId")
                .build())
        .build();

    return Tool.newBuilder()
        .addFunctionDeclarations(addCustomRoutine)
        .build();
  }

  /**
   * GenerativeModel 객체를 Spring Bean으로 등록합니다.
   * 
   * @param vertexAI (자동으로 주입됨)
   * @return GenerativeModel 싱글톤 빈
   */
  @Bean
  public GenerativeModel generativeModel(VertexAI vertexAI) {

    // 시스템 프롬프트 정의

    return new GenerativeModel.Builder()
        .setModelName(modelName)
        .setVertexAi(vertexAI)
        .setTools(Collections.singletonList(functionCallingTool()))
        .build();
  }
}