package com.be9expensphie.household.repository;

import com.be9expensphie.household.entity.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserSummaryRepository extends JpaRepository<UserSummary, Long> {
    Optional<UserSummary> findByUserId(Long userId);

    // Batch form for getMembers, which otherwise resolves one name per member
    // in its own query — and, with no transaction around it, its own
    // transaction as well.
    List<UserSummary> findByUserIdIn(Collection<Long> userIds);
}
