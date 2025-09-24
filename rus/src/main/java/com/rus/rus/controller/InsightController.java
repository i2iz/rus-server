package com.rus.rus.controller;

import com.rus.rus.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/insight")
@RequiredArgsConstructor
public class InsightController {

    /**
     * 사용자 데이터에 대한 단발성 인사이트를 반환합니다.
     * @param uid 사용자 uid (String)
     * @param userDetails Authentication된 사용자의 정보
     * @return 통합된 사용자 데이터 (JSON 텍스트)
     */
    @GetMapping(value = "/{uid}")
    public ResponseEntity<Map<String, String>> getUserInsightData(
            @PathVariable("uid") String uid,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String currentUserId = userDetails.getUsername();

        if (!currentUserId.equals(uid)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 외 사용자의 정보는 열람할 수 없습니다.");
        }

        // 1. 응답에 포함될 현재 시간
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        
        // 2. 테스트 결과 데이터
        String markdownResult = "## 테스트 데이터 입니다.\n\n";

        // 3. HashMap에 데이터 담기
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("timestamp", formattedDateTime);
        responseMap.put("resultData", markdownResult);
        
        return ResponseEntity.ok(responseMap);
    }
}