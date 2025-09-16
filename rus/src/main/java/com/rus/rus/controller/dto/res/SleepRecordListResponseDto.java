package com.rus.rus.controller.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SleepRecordListResponseDto {

    @JsonProperty("sleepRecords")
    private final List<RecordSleepResponseDto> sleepRecords;
}