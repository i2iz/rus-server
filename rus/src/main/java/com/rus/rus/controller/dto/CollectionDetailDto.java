package com.rus.rus.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class CollectionDetailDto {
    private Integer collectionId;
    private String title;
    private String subTitle;
    private String guide;
    private List<RoutineDto> routines;
}
