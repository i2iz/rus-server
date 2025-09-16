package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "users_routine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private UserProfile userProfile; // FK to users_profile

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // FK to categorys

    @Column(name = "content")
    private String content; // 루틴 내용

    // 문서에 타입이 '?'로 명시되어 있어, 알림 시간이므로 LocalTime으로 가정
    @Column(name = "notification")
    private LocalTime notification;
}
