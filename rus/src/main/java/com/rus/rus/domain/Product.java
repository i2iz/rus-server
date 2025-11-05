package com.rus.rus.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "product_id")
  private Integer productId; // 상품 아이디 (PK)

  @Column(name = "brand_name", nullable = false)
  private String brandName; // 브랜드 명

  @Column(name = "product_name", nullable = false)
  private String productName; // 상품 이름

  @Column(name = "price", nullable = false)
  private Integer price; // 상품 가격 (포인트)

  @Column(name = "image_url")
  private String imageUrl; // 상품 이미지 (URL)
}