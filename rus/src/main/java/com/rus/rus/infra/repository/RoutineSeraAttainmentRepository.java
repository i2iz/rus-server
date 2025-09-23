package com.rus.rus.infra.repository;

import com.rus.rus.domain.RoutineSeraAttainment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoutineSeraAttainmentRepository extends JpaRepository<RoutineSeraAttainment, Long> {
   // List<RoutineSeraAttainment> findByUidAndTimestampBetween(String uid, LocalDateTime start, LocalDateTime end);
   // boolean existsByRidAndUidAndTimestampBetween(Integer rid, String uid, LocalDateTime start, LocalDateTime end);
    List<RoutineSeraAttainment> findByUserProfile_UidAndTimestampBetween(
            String uid,
            LocalDateTime start,
            LocalDateTime end
    );

    boolean existsByUserProfile_UidAndRoutineSera_IdAndTimestampBetween(
            String uid,
            Integer id,
            LocalDateTime start,
            LocalDateTime end
    );
}
