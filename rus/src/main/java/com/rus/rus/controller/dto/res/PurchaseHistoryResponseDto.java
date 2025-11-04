package com.rus.rus.controller.dto.res;

import com.rus.rus.domain.Product;
import com.rus.rus.domain.PurchaseHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PurchaseHistoryResponseDto {

  // --- 구매 내역 (PurchaseHistory) ---
  private Long purchaseId;
  private LocalDateTime purchasedAt;
  private LocalDate expiresAt;
  private Boolean used;
  private Boolean expired;

  // --- 상품 정보 (Product) ---
  private Integer productId;
  private String brandName;
  private String productName;
  private Integer pricePaid; // 구매 당시 가격
  private String imageUrl;

  /**
   * PurchaseHistory 엔티티를 DTO로 변환합니다.
   */
  public static PurchaseHistoryResponseDto from(PurchaseHistory history) {
    Product product = history.getProduct();

    return PurchaseHistoryResponseDto.builder()
        // 구매 내역
        .purchaseId(history.getPurchaseId())
        .purchasedAt(history.getPurchasedAt())
        .expiresAt(history.getExpiresAt())
        .used(history.getUsed())
        .expired(history.getExpired())
        // 상품 정보
        .productId(product.getProductId())
        .brandName(product.getBrandName())
        .productName(product.getProductName())
        .pricePaid(product.getPrice())
        .imageUrl(product.getImageUrl())
        .build();
  }
}