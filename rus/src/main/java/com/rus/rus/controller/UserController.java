package com.rus.rus.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rus.rus.application.StatisticsService;
import com.rus.rus.application.UserService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.EditProfileRequestDto;
import com.rus.rus.controller.dto.req.EditSettingRequestDto;
import com.rus.rus.controller.dto.res.StatisticsResponseDto;
import com.rus.rus.controller.dto.res.UserProfileResponseDto;
import com.rus.rus.controller.dto.res.UserSettingResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StatisticsService statisticsService;

    /**
     * 사용자 프로필 조회
     * - 현재 로그인한 사용자의 프로필 정보를 조회합니다.
     * @param uid 사용자 uid
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return
     */
    @GetMapping("/profile/{uid}")
    public ResponseEntity < UserProfileResponseDto > getUserProfile(
        @PathVariable("uid") UUID uid,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());

        System.out.println(currentUserId);

        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 외 사용자의 정보는 열람할 수 없습니다");
        }

        UserProfileResponseDto dto = userService.getUserProfileById(uid);
        return ResponseEntity.ok(dto);
    }

    /**
     * 사용자 설정값 조회
     * - 현재 로그인한 사용자의 UI 및 칭호 설정값 정보를 반환합니다.
     * @param uid 사용자 uid
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return
     */
    @GetMapping("/settings/{uid}")
    public ResponseEntity < UserSettingResponseDto > getUserSetting(
        @PathVariable("uid") UUID uid,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());

        System.out.println(currentUserId);

        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 외 사용자의 정보는 열람할 수 없습니다");
        }

        UserSettingResponseDto dto = userService.getUserSettingById(uid);
        return ResponseEntity.ok(dto);
    }

    /**
     * 사용자 프로필 수정
     * 프로필 정보(이름, 생년월일, 성별, 키&몸무게)를 수정합니다.
     * @param entity 수정할 정보
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @param uid 사용자 uid
     * @return
     */
    @PatchMapping("/profile/{uid}")
    public ResponseEntity < Void > editProfile(
        @RequestBody EditProfileRequestDto entity,
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable("uid") UUID uid) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());

        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 외 사용자의 정보는 열람할 수 없습니다");
        }

        // entity에 대한 유효성 검사
        if (entity.getName() == null || entity.getName().isEmpty() ||
            entity.getBirthDate() == null || entity.getGender() == null || entity.getGender().isBlank() || entity.getHeight() == null || entity.getWeight() == null || entity.getHeight() <= 0 || entity.getWeight() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "수정 값은 null일 수 없습니다.");
        }

        userService.editUserProfile(uid, entity);

        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 설정값 수정
     * @param entity 수정할 정보
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @param uid 사용자 uid
     * @return
     */
    @PatchMapping("/settings/{uid}")
    public ResponseEntity < Void > editUserSetting(
        @RequestBody EditSettingRequestDto entity,
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable("uid") UUID uid) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());

        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 외 사용자의 정보는 열람할 수 없습니다");
        }

        // entity에 대한 유효성 검사
        if (entity.getTitleId() == null ||
            entity.getBackgroundColor() == null ||
            entity.getLumiImage() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "수정 값은 null일 수 없습니다.");
        }

        if (entity.getBackgroundColor() > 10 || entity.getLumiImage() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "유효하지 않은 설정값입니다");
        }

        userService.editUserSetting(uid, entity);

        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 루틴 달성 통계 반환
     * @param uid 사용자 uid
     * @param startDay 조회 시작일(YYYY-MM-DD)
     * @param endDay 조회 종료일(YYYY-MM-DD)
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return
     */
    @GetMapping("/statistics/{uid}")
    public ResponseEntity < StatisticsResponseDto > getUserRoutineStatistics(
        @PathVariable("uid") UUID uid,
        @RequestParam("startDay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDay,
        @RequestParam("endDay") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDay,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 본인의 데이터만 조회할 수 있도록 검증합니다.
        UUID currentUserId = UUID.fromString(userDetails.getUsername());
        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인의 통계만 조회할 수 있습니다.");
        }

        // 사용자의 통계 기록 조회
        StatisticsResponseDto responseDto = statisticsService.getUserRoutineStatistics(uid, startDay, endDay);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 사용자의 첫 로그인 상태를 true로 변경
     * @param uid 사용자 uid
     * @param userDetails Authentication된 사용자의 정보가 저장
     * @return
     */
    @PatchMapping("/isfirstlogin/{uid}")
    public ResponseEntity<Void> updateIsFirstLogin(
        @PathVariable("uid") UUID uid,
        @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = UUID.fromString(userDetails.getUsername());

        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 외 사용자의 정보는 수정할 수 없습니다.");
        }

        userService.updateIsFirstLogin(uid);

        return ResponseEntity.noContent().build();
    }
}