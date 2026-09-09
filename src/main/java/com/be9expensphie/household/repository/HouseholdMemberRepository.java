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

/**
 * Every query here except findAnyByUserIdAndHousehold excludes members whose
 * removedAt is set. Dropping that predicate from any of them makes a removed
 * member visible again.
 */
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {

    @Query("select m from HouseholdMember m where m.household.id = :householdId and m.removedAt is null")
    List<HouseholdMember> findByHouseholdId(@Param("householdId") Long householdId);

    @Query("select m from HouseholdMember m join fetch m.household where m.userId = :userId and m.removedAt is null")
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
           where m.userId = :userId and m.removedAt is null
           """)
    List<HouseholdDTO> findHouseholdsOfUser(@Param("userId") Long userId);

    /**
     * The one deliberately unfiltered lookup, used only by joinHousehold().
     *
     * A re-join has to find its own tombstone: the (household_id, user_id)
     * unique constraint would reject an insert while that row exists, so the
     * join path clears removedAt instead. Named "Any" so a filtered lookup is
     * never reached for by accident.
     */
    @Query("select m from HouseholdMember m where m.userId = :userId and m.household = :household")
    Optional<HouseholdMember> findAnyByUserIdAndHousehold(@Param("userId") Long userId,
                                                          @Param("household") Household household);

    Optional<HouseholdMember> findByUserIdAndHouseholdIdAndRemovedAtIsNull(Long userId, Long householdId);

    Optional<HouseholdMember> findByIdAndHouseholdIdAndRemovedAtIsNull(Long id, Long householdId);

    /** Backs the last-admin guard: a household must never be left without one. */
    long countByHouseholdIdAndRoleAndRemovedAtIsNull(Long householdId, HouseholdRole role);
}
