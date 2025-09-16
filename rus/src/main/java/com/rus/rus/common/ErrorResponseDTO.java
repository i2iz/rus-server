package com.rus.rus.common;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;

/**
 * 에러 메세지 DTO
 */
@Getter
public class ErrorResponseDTO {
  private final int status;
  private final String message;
  public String timestamp;

  public ErrorResponseDTO(int httpStatus, String message) {
    this.status = httpStatus;
    this.message = message;
    // 시간을 한국 시간대로 설정
    ZonedDateTime nowSeoulTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
    this.timestamp = nowSeoulTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }
}
