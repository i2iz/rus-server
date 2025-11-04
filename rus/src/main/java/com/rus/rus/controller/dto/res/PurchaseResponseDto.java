package com.rus.rus.controller.dto.res;

import com.rus.rus.domain.Product;
import com.rus.rus.domain.PurchaseHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PurchaseResponseDto {

  private Long purchaseId; // 구매 기록 ID
  private String productName; // 구매한 상품명
  private Integer pricePaid; // 지불한 가격
  private LocalDate expiresAt; // 유효기간
  private Integer updatedUserPoints; // 갱신된 사용자 포인트

  /**
   * 구매 내역과 갱신된 포인트를 DTO로 변환합니다.
   */
  public static PurchaseResponseDto from(PurchaseHistory purchaseHistory, Integer updatedPoints) {
    Product product = purchaseHistory.getProduct();

    return PurchaseResponseDto.builder()
        .purchaseId(purchaseHistory.getPurchaseId())
        .productName(product.getProductName())
        .pricePaid(product.getPrice())
        .expiresAt(purchaseHistory.getExpiresAt())
        .updatedUserPoints(updatedPoints)
        .build();
  }
}