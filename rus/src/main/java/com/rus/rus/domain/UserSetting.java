package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting {

    @Id
    @Column(length = 255)
    private String uid;  // users_profile의 uid를 FK로 참조하며 PK 역할도 수행

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "uid")
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id", referencedColumnName = "title_id")
    private Title title;

    private Integer backgroundColor;

    private Integer lumiImage;
}
