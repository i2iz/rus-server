package com.rus.rus.application;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.res.KfoodDetectionResponseDto;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class KfoodService {

  private final WebClient webClient;
  private static final String PREDICT_PATH = "/predict";

  public KfoodService(WebClient.Builder webClientBuilder,
      @Value("${kfood.api.base-url}") String kfoodApiBaseUrl) {
    // 대용량 파일 처리를 위한 타임아웃 설정
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
        .responseTimeout(Duration.ofSeconds(120))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS))
            .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));

    // 메모리 버퍼 크기 증가 (Base64 인코딩된 이미지 응답을 위해)
    ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize(50 * 1024 * 1024)) // 50MB
        .build();

    this.webClient = webClientBuilder
        .baseUrl(kfoodApiBaseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .exchangeStrategies(strategies)
        .build();
  }

  /**
   * MultipartFile을 FastAPI 객체 탐지 서버로 전달하고 결과를 받아옵니다.
   * 
   * @param file 클라이언트로부터 받은 이미지 파일
   * @return 객체 감지 결과 DTO
   */
  public KfoodDetectionResponseDto detectObjects(MultipartFile file) {
    try {
      // 1. Multipart 요청 본문 구성
      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      // key는 'file'로 설정하고, 이미지 파일의 리소스를 전송합니다.
      builder.part("file", file.getResource())
          .contentType(MediaType.IMAGE_JPEG);

      // 2. 외부 AI 서버 호출 (POST /predict)
      return webClient.post()
          .uri(PREDICT_PATH)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(builder.build()))
          .retrieve()
          .bodyToMono(KfoodDetectionResponseDto.class)
          .block(); // 동기적으로 결과를 기다립니다.

    } catch (WebClientResponseException e) {
      // 외부 서버에서 발생한 HTTP 오류 처리 (4xx, 5xx 상태 코드)
      System.err.println("K-Food API HTTP 오류 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
          "K-Food 객체 감지 서비스 오류: " + e.getResponseBodyAsString());
    } catch (Exception e) {
      // 타임아웃 또는 연결 거부 오류 처리 (API 서버가 꺼져 있을 때)
      String errorMessage = "이미지 처리 또는 서비스 접속 중 알 수 없는 오류가 발생했습니다: " + e.getMessage();

      // 타임아웃 오류 (ResponseTimeoutException, ConnectTimeoutException 등) 또는 연결 거부 확인
      if (e.getMessage().contains("timeout") || e.getMessage().contains("Connection refused")) {
        errorMessage = "AI 서비스에 접속할 수 없거나 요청 시간이 초과되었습니다. 서버 상태를 확인하세요.";
      }

      System.err.println("K-Food API 호출 중 오류: " + e.getMessage());
      // 서비스 이용 불가(503) 상태 코드로 즉시 응답
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, errorMessage);
    }
  }
}