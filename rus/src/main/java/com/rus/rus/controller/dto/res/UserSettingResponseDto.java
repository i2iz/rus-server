package com.rus.rus.controller.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rus.rus.domain.Title;
import com.rus.rus.domain.UserSetting;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSettingResponseDto {

  private final TitleDto title;

    @JsonProperty("background_color")
    private final Integer backgroundColor;

    @JsonProperty("lumi_image")
    private final Integer lumiImage;

    // 내부 Title DTO
    @Getter
    @Builder
    private static class TitleDto {
        @JsonProperty("title_id")
        private final Integer titleId;
        private final String value;

        // Title 엔티티로부터 TitleDto를 생성하는 정적 팩토리 메소드
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

    // UserSetting 엔티티로부터 UserSettingResponseDto를 생성하는 정적 팩토리 메소드
    public static UserSettingResponseDto from(UserSetting userSetting) {
        return UserSettingResponseDto.builder()
                .title(TitleDto.from(userSetting.getTitle()))
                .backgroundColor(userSetting.getBackgroundColor())
                .lumiImage(userSetting.getLumiImage())
                .build();
    }
}
