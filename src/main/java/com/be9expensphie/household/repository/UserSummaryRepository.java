package com.be9expensphie.household.repository;

import com.be9expensphie.household.entity.UserSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSummaryRepository extends JpaRepository<UserSummary, Long> {
    Optional<UserSummary> findByUserId(Long userId);
}
