package com.rus.rus.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.rus.rus.domain.WeeklyAttendance;

@Repository
public interface WeeklyAttendanceRepository extends JpaRepository<WeeklyAttendance, String> {
  /**
   * 모든 사용자의 주간 출석부 데이터를 초기화합니다.
   */
  @Modifying
  @Query("UPDATE WeeklyAttendance wa SET wa.mon = null, wa.tue = null, wa.wed = null, wa.thu = null, wa.fri = null, wa.sat = null, wa.sun = null")
  void resetAllAttendance();
}
