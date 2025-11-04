package com.rus.rus.application;

import com.rus.rus.common.ApiException;
import com.rus.rus.common.ErrorResponseDTO;
import com.rus.rus.controller.dto.res.ProductResponseDto;
import com.rus.rus.domain.Product;
import com.rus.rus.infra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // CUD 작업이 없으므로 readOnly로 설정
@RequiredArgsConstructor
public class ShopService {

  private final ProductRepository productRepository;

  /**
   * 등록된 모든 상품 목록을 조회합니다.
   * 
   * @return 상품 DTO 리스트
   */
  public List<ProductResponseDto> getAllProducts() {
    // 1. DB에서 모든 Product 엔티티를 조회합니다.
    List<Product> products = productRepository.findAll();

    // 2. Product 엔티티 리스트를 ProductResponseDto 리스트로 변환합니다.
    return products.stream()
        .map(ProductResponseDto::from)
        .collect(Collectors.toList());
  }

  /**
   * ID로 단일 상품의 상세 정보를 조회합니다.
   * 
   * @param productId 상품 ID
   * @return 상품 DTO
   * @throws ApiException 상품을 찾지 못한 경우
   */
  public ProductResponseDto getProductById(Integer productId) {
    // 1. ID로 Product 엔티티를 조회합니다.
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."));

    // 2. Product 엔티티를 ProductResponseDto로 변환하여 반환합니다.
    return ProductResponseDto.from(product);
  }
}