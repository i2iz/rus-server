package com.rus.rus.controller;

import com.rus.rus.application.KfoodService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.res.KfoodDetectionResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/kfood")
@RequiredArgsConstructor
public class KfoodController {

  private final KfoodService kfoodService;

  /**
   * 한국 음식 이미지 객체 감지 프록시 API
   * - 클라이언트로부터 받은 이미지를 FastAPI 객체 감지 서버로 전달하고, 결과를 반환합니다.
   * 
   * @param file 분석할 이미지 파일
   * @return 객체 감지 서버의 응답 (JSON)
   */
  @PostMapping("/detect")
  public ResponseEntity<KfoodDetectionResponseDto> detectKfood(
      @RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "분석할 이미지 파일이 없습니다.");
    }

    log.info("이미지 분석 요청 - 파일명: {}, 크기: {} bytes",
        file.getOriginalFilename(), file.getSize());

    try {
      KfoodDetectionResponseDto response = kfoodService.detectObjects(file);
      log.info("이미지 분석 성공 - 감지된 음식: {}", response.getDetectedFoodLabels());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("이미지 분석 실패: {}", e.getMessage(), e);
      throw e;
    }
  }
}
