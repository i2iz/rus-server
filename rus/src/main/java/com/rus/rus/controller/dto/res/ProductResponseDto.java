package com.rus.rus.controller.dto.res;

import com.rus.rus.domain.Product;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductResponseDto {

  private Integer productId;
  private String brandName;
  private String productName;
  private Integer price; // 상품 가격 (포인트)
  private String imageUrl;

  /**
   * Product 엔티티를 ProductResponseDto로 변환합니다.
   */
  public static ProductResponseDto from(Product product) {
    return ProductResponseDto.builder()
        .productId(product.getProductId())
        .brandName(product.getBrandName())
        .productName(product.getProductName())
        .price(product.getPrice())
        .imageUrl(product.getImageUrl())
        .build();
  }
}