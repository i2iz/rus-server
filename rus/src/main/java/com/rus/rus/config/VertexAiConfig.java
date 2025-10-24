package com.rus.rus.config; // (패키지 경로는 예시입니다)

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

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
   * GenerativeModel 객체를 Spring Bean으로 등록합니다.
   * 
   * @param vertexAI (자동으로 주입됨)
   * @return GenerativeModel 싱글톤 빈
   */
  @Bean
  public GenerativeModel generativeModel(VertexAI vertexAI) {
    return new GenerativeModel.Builder()
        .setModelName(modelName)
        .setVertexAi(vertexAI)
        // .setTools(...) // (나중에 함수 호출 기능을 여기에 추가)
        .build();
  }
}