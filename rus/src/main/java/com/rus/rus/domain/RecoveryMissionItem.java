package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recovery_mission_items")
@Data  // 이 어노테이션이 getter/setter를 생성합니다
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryMissionItem {

    @Id  // jakarta.persistence.Id 사용
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_mission_id", nullable = false)
    private RecoveryMission recoveryMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rid", nullable = false)
    private Routine routine;

    @Builder.Default
    @Column(name = "completed", nullable = false)
    private Boolean completed = false;  // 하나의 completed 필드만 유지
}