package com.be9expensphie.household.producer;

import com.be9expensphie.common.event.HouseholdMemberEvent;
import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.household.entity.UserSummary;
import com.be9expensphie.common.enums.HouseholdRole;
import com.be9expensphie.household.repository.UserSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The split that closes the dual write: publish() builds and stages, and
 * nothing reaches Kafka until the transaction it ran in has committed.
 *
 * Previously publish() called kafkaTemplate.send directly from inside the
 * caller's open transaction, so a rollback afterwards left expense-service
 * holding a membership change the database had discarded -- which, because
 * membership is what authorizes expense reads, meant access granted or revoked
 * on the strength of something that never happened.
 *
 * KafkaTemplate is subclassed rather than mocked: concrete classes cannot be
 * instrumented by the inline mock maker on JDK 25. Its producer factory never
 * creates a producer here, so no broker is contacted.
 */
@ExtendWith(MockitoExtension.class)
class HouseholdMemberEventProducerTest {

    private static final long HOUSEHOLD_ID = 3L;
    private static final long USER_ID = 7L;
    private static final long MEMBER_ID = 11L;

    @Mock private UserSummaryRepository userSummaryRepository;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private RecordingTemplate template;
    private HouseholdMemberEventProducer producer;

    private static class RecordingTemplate extends KafkaTemplate<String, HouseholdMemberEvent> {
        final List<String> sent = new ArrayList<>();

        RecordingTemplate() {
            super(new DefaultKafkaProducerFactory<>(Map.of()));
        }

        @Override
        public CompletableFuture<SendResult<String, HouseholdMemberEvent>> send(
                String topic, String key, HouseholdMemberEvent data) {
            sent.add(topic + "|" + key + "|" + data.getEventType());
            return CompletableFuture.completedFuture(null);
        }
    }

    @BeforeEach
    void setUp() {
        template = new RecordingTemplate();
        producer = new HouseholdMemberEventProducer(
                template, userSummaryRepository, applicationEventPublisher);
    }

    private static Household household() {
        return Household.builder().id(HOUSEHOLD_ID).name("Flat").build();
    }

    private static HouseholdMember member() {
        return HouseholdMember.builder()
                .id(MEMBER_ID)
                .userId(USER_ID)
                .role(HouseholdRole.ROLE_MEMBER)
                .household(household())
                .build();
    }

    private HouseholdMemberEventProducer.HouseholdMemberChanged staged() {
        ArgumentCaptor<HouseholdMemberEventProducer.HouseholdMemberChanged> captor =
                ArgumentCaptor.forClass(HouseholdMemberEventProducer.HouseholdMemberChanged.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    void publishStagesTheEventAndSendsNothingYet() {
        when(userSummaryRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                UserSummary.builder().userId(USER_ID).email("dana@example.com").fullName("Dana").build()));

        producer.publish(member(), household(), "MEMBER_JOINED");

        assertThat(template.sent).isEmpty();

        HouseholdMemberEvent event = staged().event();
        assertThat(event.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(event.getHouseholdId()).isEqualTo(HOUSEHOLD_ID);
        assertThat(event.getUserId()).isEqualTo(USER_ID);
        assertThat(event.getFullName()).isEqualTo("Dana");
        assertThat(event.getEmail()).isEqualTo("dana@example.com");
        assertThat(event.getRole()).isEqualTo("ROLE_MEMBER");
        assertThat(event.getEventType()).isEqualTo("MEMBER_JOINED");
    }

    /*
     * The projection lookup has to happen while the transaction is open, not in
     * the after-commit callback: the member entity is detached by then, and a
     * user_summary row written in this same transaction would not be visible to
     * a lookup that ran before it committed.
     */
    @Test
    void anUnknownUserSummaryStagesBlanksRatherThanFailing() {
        when(userSummaryRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        producer.publish(member(), household(), "MEMBER_LEFT");

        HouseholdMemberEvent event = staged().event();
        assertThat(event.getFullName()).isEmpty();
        assertThat(event.getEmail()).isEmpty();
        assertThat(event.getEventType()).isEqualTo("MEMBER_LEFT");
    }

    @Test
    void theKeyIsTheUserIdSoAMembersEventsStayInOrder() {
        when(userSummaryRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        producer.publish(member(), household(), "MEMBER_JOINED");

        assertThat(staged().key()).isEqualTo(String.valueOf(USER_ID));
    }

    @Test
    void onlyTheAfterCommitCallbackReachesKafka() {
        producer.onCommitted(new HouseholdMemberEventProducer.HouseholdMemberChanged(
                String.valueOf(USER_ID),
                HouseholdMemberEvent.builder()
                        .memberId(MEMBER_ID).householdId(HOUSEHOLD_ID).userId(USER_ID)
                        .role("ROLE_MEMBER").eventType("MEMBER_JOINED")
                        .build()));

        assertThat(template.sent).containsExactly("household-member-events|7|MEMBER_JOINED");
    }
}
