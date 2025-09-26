package com.rus.rus.domain;

import jakarta.persistence.*;  // ✅ 올바른 import
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
// import org.springframework.data.annotation.Id;  ❌ 이 줄 삭제

@Entity
@Table(name = "recovery_missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryMission {

    @Id  // 이제 jakarta.persistence.Id를 사용
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uid", nullable = false)
    private String uid;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "original_streak", nullable = false)
    private Integer originalStreak;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}