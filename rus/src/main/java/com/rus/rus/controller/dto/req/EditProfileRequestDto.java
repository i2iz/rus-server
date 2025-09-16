package com.rus.rus.controller.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditProfileRequestDto {

    private String name;

    @JsonProperty("birth_date")
    private LocalDate birthDate;

    private String gender;

    private Double height;
    private Double weight;
}

