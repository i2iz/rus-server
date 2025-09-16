package com.rus.rus.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 직접 발생시키는 예외 메세지
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponseDTO> handleApiException(ApiException ex) {
    HttpStatus httpStatus = ex.getStatus();
    String errorMessage = ex.getMessage();

    ErrorResponseDTO errorResponse = new ErrorResponseDTO(httpStatus.value(), errorMessage);

    return new ResponseEntity<>(errorResponse, httpStatus);
  }

  // 예상치 못한 모든 예외에 대한 처리
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleAllException(Exception ex) {
    HttpStatus internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
    ErrorResponseDTO errorResponse = new ErrorResponseDTO(
      internalServerError.value(),
      "예상치 못한 에러가 발생했습니다: " + ex.getMessage() // 개발 단계에서만 메세지 노출
    );

    return new ResponseEntity<>(errorResponse, internalServerError);
  }
}
