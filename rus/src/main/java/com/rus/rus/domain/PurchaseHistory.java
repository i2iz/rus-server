package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "purchase_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "purchase_id")
  private Long purchaseId; // 구매 기록 아이디 (PK, 로그성 테이블이므로 Long)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uid", nullable = false)
  private UserProfile userProfile; // '누가' (FK to users_profile)

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product; // '어떤 상품을' (FK to products)

  @Column(name = "purchased_at", nullable = false)
  private LocalDateTime purchasedAt; // '언제' 구매했는지

  @Column(name = "expires_at", nullable = false)
  private LocalDate expiresAt; // '유효기간이 언제까지'

  @Builder.Default
  @Column(name = "is_used", nullable = false)
  private Boolean used = false; // '사용했는지 아닌지'

  @Builder.Default
  @Column(name = "is_expired", nullable = false)
  private Boolean expired = false; // '유효기간이 지났는지 아닌지'

  @PrePersist
  public void prePersist() {
    if (this.purchasedAt == null) {
      // UserProfile 엔티티의 prePersist 로직과 동일하게 서울 시간대 적용
      this.purchasedAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();
    }
  }
}