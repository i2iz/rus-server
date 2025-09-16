package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="users_sleep")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSleep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sleep_log_id")
    private Long sleepLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid")
    private UserProfile userProfile;

    @Column(nullable = false)
    private LocalDate timestamp;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer sleepDuration; // in minutes
}
