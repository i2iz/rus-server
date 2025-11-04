package com.rus.rus.application;

import com.rus.rus.common.ApiException;
import com.rus.rus.common.ErrorResponseDTO;
import com.rus.rus.controller.dto.req.PurchaseRequestDto;
import com.rus.rus.controller.dto.res.ProductResponseDto;
import com.rus.rus.controller.dto.res.PurchaseResponseDto;
import com.rus.rus.domain.Product;
import com.rus.rus.domain.PurchaseHistory;
import com.rus.rus.domain.UserProfile;
import com.rus.rus.infra.repository.ProductRepository;
import com.rus.rus.infra.repository.PurchaseHistoryRepository;
import com.rus.rus.infra.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {

  private final ProductRepository productRepository;
  private final UserProfileRepository userProfileRepository;
  private final PurchaseHistoryRepository purchaseHistoryRepository;

  /**
   * 등록된 모든 상품 목록을 조회합니다.
   * 
   * @return 상품 DTO 리스트
   */
  @Transactional(readOnly = true)
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
  @Transactional(readOnly = true)
  public ProductResponseDto getProductById(Integer productId) {
    // 1. ID로 Product 엔티티를 조회합니다.
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."));

    // 2. Product 엔티티를 ProductResponseDto로 변환하여 반환합니다.
    return ProductResponseDto.from(product);
  }

  /**
   * 상품을 포인트로 구매합니다
   * 
   * @param uid        사용자 ID
   * @param requestDto 구매 요청 DTO (productId 포함)
   * @return 구매 결과 DTO (갱신된 포인트 포함)
   */
  @Transactional // (readOnly = false)
  public PurchaseResponseDto purchaseProduct(String uid, PurchaseRequestDto requestDto) {
    // 1. 사용자 조회
    UserProfile userProfile = userProfileRepository.findById(uid)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."));

    // 2. 상품 조회
    Product product = productRepository.findById(requestDto.getProductId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "해당 상품을 찾을 수 없습니다."));

    // 3. 포인트 검증
    if (userProfile.getPoint() < product.getPrice()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다");
    }

    // 4. 포인트 차감
    int newPointTotal = userProfile.getPoint() - product.getPrice();
    userProfile.setPoint(newPointTotal);

    // 5. 구매 기록 생성
    // (유효기간: UserProfile의 prePersist와 동일하게 KST 기준 30일 후로 설정)
    LocalDate expiresAt = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(30);

    PurchaseHistory newPurchase = PurchaseHistory.builder()
        .userProfile(userProfile)
        .product(product)
        .expiresAt(expiresAt)
        .build(); // used, expired는 false가 기본값

    PurchaseHistory savedPurchase = purchaseHistoryRepository.save(newPurchase);

    // 6. 결과 반환
    return PurchaseResponseDto.from(savedPurchase, newPointTotal);
  }
}