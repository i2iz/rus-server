package com.rus.rus.domain.idClass;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoutineChallengeId implements Serializable {
    private Integer category;
    private String content;
}
