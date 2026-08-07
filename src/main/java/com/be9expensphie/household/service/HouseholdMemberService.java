package com.be9expensphie.household.service;

import com.be9expensphie.household.dto.MemberDTO;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.entity.UserSummary;
import com.be9expensphie.household.repository.HouseholdMemberRepository;
import com.be9expensphie.household.repository.HouseholdRepository;
import com.be9expensphie.household.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HouseholdMemberService {

    private final HouseholdMemberRepository memberRepo;
    private final HouseholdRepository householdRepo;
    private final UserSummaryRepository userSummaryRepository;

    // readOnly, and one transaction rather than one per statement: without any
    // transaction each repository call below opened and committed its own.
    @Transactional(readOnly = true)
    public List<MemberDTO> getMembers(Long householdId, Long requestingUserId) {
        householdRepo.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        memberRepo.findByUserIdAndHouseholdId(requestingUserId, householdId)
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
}
