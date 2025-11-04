package com.rus.rus.infra.repository;

import com.rus.rus.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
  // JpaRepository의 기본 findAll() 메서드를 사용하여 모든 상품을 조회합니다.
}