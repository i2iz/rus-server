package com.rus.rus.application;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.RecordSleepRequestDto;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.domain.UserSleep;
import com.rus.rus.infra.repository.UserProfileRepository;
import com.rus.rus.infra.repository.UserSleepRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final UserSleepRepository userSleepRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * 사용자의 수면 시간을 기록합니다.
     * 오늘 날짜의 기록이 이미 존재하면 수정하고, 없으면 새로 생성합니다.
     *
     * @param uid        사용자 고유 식별자
     * @param requestDto 수면 시작 및 종료 시간 정보
     * @return 저장되거나 수정된 UserSleep 엔티티
     */
    @Transactional
    public UserSleep recordSleep(UUID uid, RecordSleepRequestDto requestDto) {
        UserProfile userProfile = userProfileRepository.findById(uid.toString())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (requestDto.getStartTime() == null || requestDto.getEndTime() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "시작 시간과 종료 시간은 필수입니다.");
        }

        LocalDate today = LocalDate.now();

        // 오늘 날짜로 기록된 수면 데이터가 있는지 확인
        Optional<UserSleep> existingSleepRecord = userSleepRepository.findTopByUserProfileUidAndTimestampOrderBySleepLogIdDesc(uid.toString(), today);

        int sleepDurationInSeconds = (int) Duration.between(requestDto.getStartTime(), requestDto.getEndTime()).getSeconds();

        UserSleep userSleep;
        if (existingSleepRecord.isPresent()) {
            // 데이터가 존재하면, 해당 기록을 업데이트합니다.
            userSleep = existingSleepRecord.get();
            userSleep.setStartTime(requestDto.getStartTime());
            userSleep.setEndTime(requestDto.getEndTime());
            userSleep.setSleepDuration(sleepDurationInSeconds);
        } else {
            // 데이터가 없으면, 새로 생성합니다.
            userSleep = UserSleep.builder()
                    .userProfile(userProfile)
                    .timestamp(today)
                    .startTime(requestDto.getStartTime())
                    .endTime(requestDto.getEndTime())
                    .sleepDuration(sleepDurationInSeconds)
                    .build();
        }

        return userSleepRepository.save(userSleep);
    }

    /**
     * 오늘 날짜에 해당하는 사용자의 수면 기록을 조회합니다.
     *
     * @param uid 사용자 고유 식별자
     * @return 조회된 UserSleep 엔티티
     */
    @Transactional(readOnly = true)
    public UserSleep getSleepRecordForToday(UUID uid) {
        // 오늘 날짜로 기록된 가장 최신 데이터를 조회합니다.
        return userSleepRepository.findTopByUserProfileUidAndTimestampOrderBySleepLogIdDesc(uid.toString(), LocalDate.now())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "오늘의 수면 기록을 찾을 수 없습니다."));
    }

    /**
     * 특정 기간 동안의 사용자의 모든 수면 기록을 조회합니다.
     *
     * @param uid       사용자 고유 식별자
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 수면 기록 리스트
     */
    @Transactional(readOnly = true)
    public List<UserSleep> getSleepRecordsForPeriod(UUID uid, LocalDate startDate, LocalDate endDate) {
        return userSleepRepository.findAllByUserProfileUidAndTimestampBetween(uid.toString(), startDate, endDate);
    }
}