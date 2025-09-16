package com.rus.rus.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rus.rus.domain.Routine;

import java.util.List;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, Integer> {
    /**
     * 카테고리 이름 목록에 해당하는 모든 루틴을 조회합니다.
     * @param categoryNames 카테고리 이름 리스트 (e.g., ["sleep", "sunlight"])
     * @return 해당하는 Routine 엔티티 리스트
     */
    @Query("SELECT r FROM Routine r WHERE r.category.value IN :categoryNames")
    List<Routine> findRoutinesByCategoryNames(@Param("categoryNames") List<String> categoryNames);
}
