package com.be9expensphie.household.repository;

import com.be9expensphie.household.dto.HouseholdDTO;
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

    // /households/my is a quarter of the read traffic and returns a flat DTO,
    // so it projects instead of loading HouseholdMember + Household entities
    // that Hibernate would have to register, snapshot for dirty checking, and
    // then discard. The join fetch above already avoided an N+1; this avoids
    // the hydration as well.
    //
    // Constructor arguments bind positionally to HouseholdDTO's
    // @AllArgsConstructor: id, name, code, role, memberId.
    @Query("""
           select new com.be9expensphie.household.dto.HouseholdDTO(
               h.id, h.name, h.code, cast(m.role as String), m.id)
           from HouseholdMember m
           join m.household h
           where m.userId = :userId
           """)
    List<HouseholdDTO> findHouseholdsOfUser(@Param("userId") Long userId);

    Optional<HouseholdMember> findByUserIdAndHousehold(Long userId, Household household);

    Optional<HouseholdMember> findByUserIdAndHouseholdId(Long userId, Long householdId);
}
