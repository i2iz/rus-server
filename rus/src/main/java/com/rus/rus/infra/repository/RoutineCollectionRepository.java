package com.rus.rus.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rus.rus.domain.RoutineCollection;

@Repository
public interface RoutineCollectionRepository extends JpaRepository<RoutineCollection, Integer> {

}
