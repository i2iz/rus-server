package com.rus.rus.domain.idClass;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoutineCollectionMapperId implements Serializable {
    private Integer routineCollection;
    private Integer routine;
}
