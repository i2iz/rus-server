package com.rus.rus.domain;

import com.rus.rus.domain.idClass.RoutineChallengeId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "routines_challenge")
@IdClass(RoutineChallengeId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutineChallenge {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // Composite PK 1, FK to categorys

    @Id
    @Column(name = "content")
    private String content; // Composite PK 2, 챌린지 내용
}
