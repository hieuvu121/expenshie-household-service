package com.be9expensphie.household.service;

import com.be9expensphie.household.dto.MemberDTO;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.repository.HouseholdMemberRepository;
import com.be9expensphie.household.repository.HouseholdRepository;
import com.be9expensphie.household.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseholdMemberService {

    private final HouseholdMemberRepository memberRepo;
    private final HouseholdRepository householdRepo;
    private final UserSummaryRepository userSummaryRepository;

    public List<MemberDTO> getMembers(Long householdId) {
        householdRepo.findById(householdId)
                .orElseThrow(() -> new RuntimeException("Household not found"));

        List<HouseholdMember> members = memberRepo.findByHouseholdId(householdId);
        return members.stream()
                .map(m -> {
                    String fullName = userSummaryRepository.findByUserId(m.getUserId())
                            .map(u -> u.getFullName())
                            .orElse("Unknown");
                    return new MemberDTO(m.getId(), fullName, m.getRole());
                })
                .toList();
    }
}
