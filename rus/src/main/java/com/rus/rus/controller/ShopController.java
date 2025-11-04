package com.rus.rus.controller;

import com.rus.rus.application.ShopService;
import com.rus.rus.controller.dto.res.ProductResponseDto;
import lombok.RequiredArgsConstructor;

import org.hibernate.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

  private final ShopService shopService;

  @GetMapping("/products")
  public List<ProductResponseDto> getAllProducts() {
    return shopService.getAllProducts();
  }

  /**
   * 단일 상품 상세 정보 조회
   * 
   * @param productId 조회할 상품의 ID
   * @return 단일 상품 DTO
   */
  @GetMapping("/products/{productId}")
  public ProductResponseDto getProductById(@PathVariable Integer productId) {
    return shopService.getProductById(productId);
  }
}