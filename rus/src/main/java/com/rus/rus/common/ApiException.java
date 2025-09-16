package com.rus.rus.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message); // RuntimeException의 메세지로 전달
        this.status = status;
    }

}
