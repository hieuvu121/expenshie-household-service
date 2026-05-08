package com.be9expensphie.household.repository;

import com.be9expensphie.household.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    boolean existsByName(String name);
    boolean existsByCode(String code);
    Optional<Household> findByCode(String code);
}
