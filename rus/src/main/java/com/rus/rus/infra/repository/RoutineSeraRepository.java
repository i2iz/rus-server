package com.rus.rus.infra.repository;

import com.rus.rus.domain.RoutineSera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface RoutineSeraRepository extends JpaRepository<RoutineSera, Integer> {
    //List<RoutineSera> findByUid(String uid);
    //Optional<RoutineSera> findById(Integer id);
    List<RoutineSera> findByUserProfile_Uid(String uid);
}
