package com.rus.rus.controller.dto.res;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

import com.rus.rus.controller.dto.CollectionDetailDto;

@Getter
@Builder
public class AllCollectionsResponseDto {
    private List<CollectionDetailDto> collections;
}
