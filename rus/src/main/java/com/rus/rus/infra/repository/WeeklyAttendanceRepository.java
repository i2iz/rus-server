package com.rus.rus.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rus.rus.domain.WeeklyAttendance;

@Repository
public interface WeeklyAttendanceRepository extends JpaRepository<WeeklyAttendance, String> {
}
