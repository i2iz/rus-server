package com.rus.rus.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name="users_profile")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

  @Id
  @Column(length = 255)
  private String uid;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(length = 10)
  private String gender;

  @Builder.Default
  @Column(nullable = false)
  private int level = 1; // 기본값 1

  @Builder.Default
  @Column(nullable = false)
  private Integer lux=0;

  @Builder.Default
  @Column(nullable = false)
  @JsonIgnore
  private boolean isFirstLogin = false;

  @Column(length = 100)
  private String name;

  private LocalDate birthDate;

  @Column(length = 255)
  private String email;

  @Builder.Default
  private Double height=0.0; // 기본값 0.0

  @Builder.Default
  private Double weight=0.0; // 기본값 0.0

  @PrePersist
  public void prePersist() {
    if(this.createdAt == null) {
      // UTC+9 기준 현재 시간으로 세팅
      this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
  }
}
