package com.be9expensphie.household.producer;

import com.be9expensphie.common.event.HouseholdMemberEvent;
import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HouseholdMemberEventProducer {
    //wrapper that helps serialize+deserialize with flow sending event
    private final KafkaTemplate<String,HouseholdMemberEvent> kafkaTemplate;
    private final UserSummaryRepository userSummaryRepository;

    public void publish(HouseholdMember member, Household household, String eventType) {
        var summary = userSummaryRepository.findByUserId(member.getUserId()).orElse(null);
        String fullName = summary != null ? summary.getFullName() : "";
        String email = summary != null ? summary.getEmail() : "";

        HouseholdMemberEvent event = HouseholdMemberEvent.builder()
                .memberId(member.getId())
                .householdId(household.getId())
                .userId(member.getUserId())
                .email(email)
                .fullName(fullName)
                .role(member.getRole().name())
                .eventType(eventType)
                .build();

        kafkaTemplate.send("household-member-events", String.valueOf(member.getUserId()), event);
        log.info("Published HouseholdMemberEvent: userId={}, householdId={}, type={}", member.getUserId(), household.getId(), eventType);
    }
}
