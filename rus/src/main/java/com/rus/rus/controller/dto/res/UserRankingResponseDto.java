package com.rus.rus.controller.dto.res;

import com.rus.rus.controller.dto.UserRankingItemDto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRankingResponseDto {
  private List<UserRankingItemDto> rankings;
}
