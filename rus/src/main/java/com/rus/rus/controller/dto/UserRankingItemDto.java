package com.rus.rus.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.domain.UserSetting;
import com.rus.rus.domain.Title;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRankingItemDto {

    private String name;
    private int level;
    private int lux;
    private TitleDto title;

    @JsonProperty("lumi_image")
    private Integer lumiImage;

    // 내부 Title DTO
    @Getter
    @Builder
    public static class TitleDto {
        @JsonProperty("title_id")
        private final Integer titleId;
        private final String value;

        public static TitleDto from(Title title) {
            if (title == null) {
                return null;
            }
            return TitleDto.builder()
                    .titleId(title.getTitleId())
                    .value(title.getValue())
                    .build();
        }
    }

    // UserProfile과 UserSetting 엔티티를 받아서 DTO를 생성하는 정적 팩토리 메소드
    public static UserRankingItemDto from(UserProfile userProfile, UserSetting userSetting) {
        return UserRankingItemDto.builder()
                .name(userProfile.getName())
                .level(userProfile.getLevel())
                .lux(userProfile.getLux())
                .title(TitleDto.from(userSetting.getTitle()))
                .lumiImage(userSetting.getLumiImage())
                .build();
    }
}