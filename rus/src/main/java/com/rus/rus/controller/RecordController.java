package com.rus.rus.controller;

import lombok.RequiredArgsConstructor;

import org.hibernate.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.rus.rus.application.RecordService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.RecordSleepRequestDto;
import com.rus.rus.controller.dto.res.RecordSleepResponseDto;
import com.rus.rus.controller.dto.res.SleepRecordListResponseDto;
import com.rus.rus.domain.UserSleep;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/record")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    /**
     * 수면 시간 저장
     * - 취침 시각과 기상 시각을 받아 수면 시간을 저장합니다.
     * @param uid 사용자 uid
     * @param requestDto 수면 시간 데이터 (시작 시간 ~ 종료 시간)
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return 
     */
    @PostMapping("/sleep/{uid}")
    public ResponseEntity<RecordSleepResponseDto> recordSleep(
            @PathVariable("uid") UUID uid,
            @RequestBody RecordSleepRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 기록만 저장할 수 있습니다.");
        }

        UserSleep recordedSleep = recordService.recordSleep(uid, requestDto);
        return new ResponseEntity<>(RecordSleepResponseDto.from(recordedSleep), HttpStatus.CREATED);
    }

    /**
     * 오늘의 수면 시간 정보 반환
     * @param uid 사용자 uid
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return 요청 날짜의 수면 시간 정보
     */
    @GetMapping("/sleep/today/{uid}")
    public ResponseEntity<RecordSleepResponseDto> getSleepRecordForToday(
            @PathVariable("uid") UUID uid,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 기록만 조회할 수 있습니다.");
        }

        UserSleep sleepRecord = recordService.getSleepRecordForToday(uid);
        return ResponseEntity.ok(RecordSleepResponseDto.from(sleepRecord));
    }

    /**
     * 특정 기간의 수면 시간 정보 반환
     * @param uid 사용자 uid
     * @param startDate 조회 시작일
     * @param endDate 조회 종료일
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return 수면 시간 정보
     */
    @GetMapping("/sleep/{uid}")
    public ResponseEntity<SleepRecordListResponseDto> getSleepRecordsForPeriod(
            @PathVariable("uid") UUID uid,
            @RequestParam("start_day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end_day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 기록만 조회할 수 있습니다.");
        }

        List<UserSleep> sleepRecords = recordService.getSleepRecordsForPeriod(uid, startDate, endDate);

        List<RecordSleepResponseDto> dtoList = sleepRecords.stream()
                .map(RecordSleepResponseDto::from)
                .collect(Collectors.toList());

        SleepRecordListResponseDto response = SleepRecordListResponseDto.builder()
                .sleepRecords(dtoList)
                .build();

        return ResponseEntity.ok(response);
    }
}