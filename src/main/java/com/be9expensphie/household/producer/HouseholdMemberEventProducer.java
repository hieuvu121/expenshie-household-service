package com.be9expensphie.household.producer;

import com.be9expensphie.common.event.HouseholdMemberEvent;
import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.repository.UserSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class HouseholdMemberEventProducer {

    /** In-process carrier. Built inside the transaction, sent once it commits. */
    public record HouseholdMemberChanged(String key, HouseholdMemberEvent event) {}

    //wrapper that helps serialize+deserialize with flow sending event
    private final KafkaTemplate<String, HouseholdMemberEvent> kafkaTemplate;
    private final UserSummaryRepository userSummaryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Builds the event and stages it. Called from inside the caller's transaction.
     *
     * The build has to stay here rather than move into the callback below. The
     * member entity is detached once the transaction ends, and the user_summary
     * lookup has to see rows this same transaction may have written.
     */
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

        applicationEventPublisher.publishEvent(
                new HouseholdMemberChanged(String.valueOf(member.getUserId()), event));
    }

    /**
     * The only path to Kafka, and it runs after the commit.
     *
     * publish() used to send from inside the open transaction. A rollback after
     * that point left expense-service holding a membership change the database
     * had discarded -- and since membership is what authorizes expense reads,
     * that is access granted or revoked on the strength of something that never
     * happened.
     *
     * Still at-most-once once the commit lands: a crash in this gap loses the
     * event and expense-service's projection drifts until that member is
     * touched again. Accepted here, unlike on expense-events, where the same
     * loss is money nobody is ever asked to settle -- that path uses an outbox.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommitted(HouseholdMemberChanged change) {
        kafkaTemplate.send("household-member-events", change.key(), change.event());
        log.info("Published HouseholdMemberEvent: userId={}, householdId={}, type={}",
                change.event().getUserId(), change.event().getHouseholdId(),
                change.event().getEventType());
    }
}
