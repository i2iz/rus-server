package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "weekly_attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyAttendance {

    @Id
    @Column(name = "uid")
    private String uid; // PK, FK to users_profile

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "uid")
    private UserProfile userProfile;

    // 아직 날짜가 되지 않은 경우 null이 가능하므로 Boolean 래퍼 타입 사용
    private Boolean mon;
    private Boolean tue;
    private Boolean wed;
    private Boolean thu;
    private Boolean fri;
    private Boolean sat;
    private Boolean sun;
}
