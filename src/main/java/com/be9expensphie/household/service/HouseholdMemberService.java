package com.be9expensphie.household.service;

import com.be9expensphie.household.dto.MemberDTO;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.entity.UserSummary;
import com.be9expensphie.household.enums.HouseholdRole;
import com.be9expensphie.household.producer.HouseholdMemberEventProducer;
import com.be9expensphie.household.repository.HouseholdMemberRepository;
import com.be9expensphie.household.repository.HouseholdRepository;
import com.be9expensphie.household.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HouseholdMemberService {

    private final HouseholdMemberRepository memberRepo;
    private final HouseholdRepository householdRepo;
    private final UserSummaryRepository userSummaryRepository;
    private final HouseholdMemberEventProducer householdMemberEventProducer;

    // readOnly, and one transaction rather than one per statement: without any
    // transaction each repository call below opened and committed its own.
    @Transactional(readOnly = true)
    public List<MemberDTO> getMembers(Long householdId, Long requestingUserId) {
        householdRepo.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        memberRepo.findByUserIdAndHouseholdIdAndRemovedAtIsNull(requestingUserId, householdId)
                .orElseThrow(() -> new RuntimeException("Access denied: not a member of this household"));

        List<HouseholdMember> members = memberRepo.findByHouseholdId(householdId);

        // One query for all the names instead of one per member. The per-row
        // lookup was an N+1 that stayed invisible only because test households
        // have a handful of members.
        List<Long> userIds = members.stream().map(HouseholdMember::getUserId).toList();
        Map<Long, String> names = userIds.isEmpty()
                ? Map.of()
                : userSummaryRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(UserSummary::getUserId, UserSummary::getFullName));

        return members.stream()
                .map(m -> new MemberDTO(m.getId(),
                        names.getOrDefault(m.getUserId(), "Unknown"),
                        m.getRole()))
                .toList();
    }

    /**
     * Removes a member, or lets one leave.
     *
     * Authorized for an admin of the household or for the member themselves —
     * "kick" and "leave" are the same operation seen from two directions, so
     * they share a path rather than duplicating the lookups.
     *
     * The row is soft-deleted (see HouseholdMember.removedAt) and MEMBER_LEFT
     * is published so expense-service can retire its own projection and drop
     * the cached membership answer.
     *
     * Outstanding settlements are deliberately not checked. They are keyed by
     * member id and survive the removal, so a debt stays visible and settleable;
     * blocking on them would mean calling settlement-service synchronously and
     * making "leave a household" fail whenever that service is down.
     */
    @Transactional
    public void removeMember(Long householdId, Long targetMemberId, Long requestingUserId) {
        HouseholdMember requester = memberRepo
                .findByUserIdAndHouseholdIdAndRemovedAtIsNull(requestingUserId, householdId)
                .orElseThrow(() -> new RuntimeException("Access denied: not a member of this household"));

        HouseholdMember target = memberRepo
                .findByIdAndHouseholdIdAndRemovedAtIsNull(targetMemberId, householdId)
                .orElseThrow(() -> new RuntimeException("Member not found in this household"));

        boolean removingSelf = target.getId().equals(requester.getId());
        if (!removingSelf && requester.getRole() != HouseholdRole.ROLE_ADMIN) {
            throw new RuntimeException("Only admin can remove another member");
        }

        // Applies to an admin leaving voluntarily as much as to one being
        // removed. Without an admin, expense-service's createExpense cannot
        // resolve a reviewer and every expense in the household fails.
        if (target.getRole() == HouseholdRole.ROLE_ADMIN
                && memberRepo.countByHouseholdIdAndRoleAndRemovedAtIsNull(householdId, HouseholdRole.ROLE_ADMIN) <= 1) {
            throw new RuntimeException("Cannot remove the last admin of this household");
        }

        target.setRemovedAt(Instant.now());
        memberRepo.save(target);

        householdMemberEventProducer.publish(target, target.getHousehold(), "MEMBER_LEFT");
    }
}
