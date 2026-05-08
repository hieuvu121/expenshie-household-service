package com.be9expensphie.household.service;

import com.be9expensphie.household.dto.CreateHouseholdDTO.CreateRequest;
import com.be9expensphie.household.dto.CreateHouseholdDTO.CreateResponse;
import com.be9expensphie.household.dto.HouseholdDTO;
import com.be9expensphie.household.dto.JoinHouseholdDTO.JoinHouseholdRequestDTO;
import com.be9expensphie.household.dto.JoinHouseholdDTO.JoinHouseholdResponseDTO;
import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.enums.HouseholdRole;
import com.be9expensphie.household.producer.HouseholdMemberEventProducer;
import com.be9expensphie.household.repository.HouseholdMemberRepository;
import com.be9expensphie.household.repository.HouseholdRepository;
import com.be9expensphie.household.repository.UserSummaryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UserSummaryRepository userSummaryRepository;
    private final HouseholdMemberEventProducer householdMemberEventProducer;

    @Transactional
    public CreateResponse create(CreateRequest request, Long userId) {
        if (householdRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("This name is already exist");
        }

        Household household = Household.builder()
                .name(request.getName())
                .createdBy(userId)
                .code(UUID.randomUUID().toString().substring(0, 8))
                .build();
        householdRepository.save(household);

        HouseholdMember member = HouseholdMember.builder()
                .household(household)
                .userId(userId)
                .role(HouseholdRole.ROLE_ADMIN)
                .build();
        householdMemberRepository.save(member);

        householdMemberEventProducer.publish(member, household, "MEMBER_JOINED", userSummaryRepository);

        return CreateResponse.builder()
                .id(household.getId())
                .name(household.getName())
                .role(member.getRole().name())
                .memberId(member.getId())
                .build();
    }

    @Transactional
    public JoinHouseholdResponseDTO joinHousehold(JoinHouseholdRequestDTO request, Long userId) {
        Household household = householdRepository.findByCode(request.getCode())
                .orElseThrow(() -> new RuntimeException("Household not found"));

        Optional<HouseholdMember> existing = householdMemberRepository.findByUserIdAndHousehold(userId, household);

        HouseholdMember member = existing.orElseGet(() -> {
            HouseholdMember newMember = HouseholdMember.builder()
                    .household(household)
                    .userId(userId)
                    .role(HouseholdRole.ROLE_MEMBER)
                    .build();
            HouseholdMember saved = householdMemberRepository.save(newMember);
            householdMemberEventProducer.publish(saved, household, "MEMBER_JOINED", userSummaryRepository);
            return saved;
        });

        return JoinHouseholdResponseDTO.builder()
                .householdId(household.getId())
                .householdName(household.getName())
                .role(member.getRole().name())
                .memberId(member.getId())
                .build();
    }

    @Transactional
    public List<HouseholdDTO> getHousehold(Long userId) {
        return householdMemberRepository.findByUserId(userId)
                .stream()
                .map(m -> HouseholdDTO.builder()
                        .id(m.getHousehold().getId())
                        .name(m.getHousehold().getName())
                        .role(m.getRole().name())
                        .code(m.getHousehold().getCode())
                        .memberId(m.getId())
                        .build())
                .toList();
    }
}
