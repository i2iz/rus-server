package com.rus.rus.controller;

import com.rus.rus.application.ShopService;
import com.rus.rus.common.ApiException;
import com.rus.rus.controller.dto.req.PurchaseRequestDto;
import com.rus.rus.controller.dto.res.ProductResponseDto;
import com.rus.rus.controller.dto.res.PurchaseResponseDto;

import lombok.RequiredArgsConstructor;

import org.hibernate.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

  /**
   * 상품 구매 (포인트 사용)
   * 
   * @param requestDto 구매할 상품 ID
   * @return 구매 결과 및 갱신된 포인트
   */
  @PostMapping("/purchase")
  public PurchaseResponseDto purchaseProduct(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody PurchaseRequestDto requestDto) {
    return shopService.purchaseProduct(userDetails.getUsername(), requestDto);
  }
}