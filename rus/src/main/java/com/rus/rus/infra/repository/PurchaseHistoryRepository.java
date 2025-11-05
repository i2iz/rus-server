package com.rus.rus.infra.repository;

import com.rus.rus.domain.PurchaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, Long> {

  /**
   * 사용자 UID로 모든 구매 내역을 조회합니다.
   * Product 정보를 Fetch Join 합니다.
   *
   * @param uid 사용자 ID
   * @return 구매 내역 리스트 (상품 정보 포함)
   */
  @Query("SELECT ph FROM PurchaseHistory ph " +
      "JOIN FETCH ph.product p " +
      "WHERE ph.userProfile.uid = :uid " +
      "AND ph.used = false " +
      "ORDER BY ph.purchasedAt DESC") // 최신 구매 순으로 정렬
  List<PurchaseHistory> findAllByUidWithProductFetchJoin(@Param("uid") String uid);
}