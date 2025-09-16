package com.rus.rus.controller.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rus.rus.domain.UserSleep;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class RecordSleepResponseDto {

    @JsonProperty("sleepLogId")
    private final Long sleepLogId;

    @JsonProperty("uid")
    private final String uid;

    @JsonProperty("timestamp")
    private final LocalDate timestamp;

    @JsonProperty("startTime")
    private final LocalDateTime startTime;

    @JsonProperty("endTime")
    private final LocalDateTime endTime;

    @JsonProperty("sleepTime")
    private final Integer sleepTime;

    public static RecordSleepResponseDto from(UserSleep userSleep) {
        return RecordSleepResponseDto.builder()
                .sleepLogId(userSleep.getSleepLogId())
                .uid(userSleep.getUserProfile().getUid())
                .timestamp(userSleep.getTimestamp())
                .startTime(userSleep.getStartTime())
                .endTime(userSleep.getEndTime())
                .sleepTime(userSleep.getSleepDuration())
                .build();
    }
}