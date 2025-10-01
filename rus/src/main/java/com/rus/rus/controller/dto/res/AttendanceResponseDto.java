package com.rus.rus.controller.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.ZonedDateTime; // LocalDateTime에서 ZonedDateTime으로 변경

@Getter
@Builder
public class AttendanceResponseDto {

    // API 명세의 "yyyy-MM-dd'T'HH:mm:ss+09:00" 형식에 맞추기 위해 포맷을 지정합니다.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private ZonedDateTime timestamp; // 필드 타입을 ZonedDateTime으로 변경

    private Boolean mon;
    private Boolean tue;
    private Boolean wed;
    private Boolean thu;
    private Boolean fri;
    private Boolean sat;
    private Boolean sun;

    private LocalDate startDate;
    private LocalDate endDate;
}
