package com.rus.rus.controller.dto.res;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rus.rus.domain.UserProfile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileResponseDto {

    private LocalDateTime created_at;
    private String gender;
    private int level;
    private int lux;
    private int point;

    @JsonProperty("is_first_login")
    private boolean isFirstLogin;

    @JsonProperty("is_first_login")
    public boolean getIsFirstLogin() {
        return isFirstLogin;
    }

    private String name;
    private LocalDate birth_date;
    private String email;

    private Double height;
    private Double weight;

    public static UserProfileResponseDto from(UserProfile profile) {
        return UserProfileResponseDto.builder()
                .created_at(profile.getCreatedAt())
                .gender(profile.getGender())
                .level(profile.getLevel())
                .lux(profile.getLux())
                .point(profile.getPoint())
                .isFirstLogin(profile.isFirstLogin())
                .name(profile.getName())
                .birth_date(profile.getBirthDate())
                .email(profile.getEmail())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .build();
    }
}
