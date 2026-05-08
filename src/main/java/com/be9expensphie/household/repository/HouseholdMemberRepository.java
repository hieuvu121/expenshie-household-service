package com.be9expensphie.household.repository;

import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.enums.HouseholdRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {

    @Query("select m from HouseholdMember m where m.household.id = :householdId")
    List<HouseholdMember> findByHouseholdId(@Param("householdId") Long householdId);

    @Query("select m from HouseholdMember m join fetch m.household where m.userId = :userId")
    List<HouseholdMember> findByUserId(@Param("userId") Long userId);

    Optional<HouseholdMember> findByUserIdAndHousehold(Long userId, Household household);

    Optional<HouseholdMember> findByUserIdAndHouseholdId(Long userId, Long householdId);
}
