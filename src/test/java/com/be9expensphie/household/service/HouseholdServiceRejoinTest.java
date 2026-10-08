package com.be9expensphie.household.service;

import com.be9expensphie.household.dto.JoinHouseholdDTO.JoinHouseholdRequestDTO;
import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.common.enums.HouseholdRole;
import com.be9expensphie.household.producer.HouseholdMemberEventProducer;
import com.be9expensphie.household.repository.HouseholdMemberRepository;
import com.be9expensphie.household.repository.HouseholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the other half of removal: a member who left and comes back.
 *
 * The (household_id, user_id) unique constraint means the tombstoned row has
 * to be reused, so these tests pin that joinHousehold clears removedAt rather
 * than inserting a second row.
 */
@ExtendWith(MockitoExtension.class)
class HouseholdServiceRejoinTest {

    private static final long HOUSEHOLD_ID = 3L;
    private static final long USER_ID = 2L;
    private static final long MEMBER_ID = 20L;
    private static final String CODE = "abc12345";

    @Mock private HouseholdRepository householdRepository;
    @Mock private HouseholdMemberRepository householdMemberRepository;
    private final RecordingProducer producer = new RecordingProducer();

    @InjectMocks private HouseholdService service;

    /*
     * @InjectMocks populates @Mock fields only, and producer is a hand-written
     * recorder rather than a mock, so it has to be set here.
     */
    @BeforeEach
    void injectRecordingProducer() {
        ReflectionTestUtils.setField(service, "householdMemberEventProducer", producer);
    }

    private Household household;

    @BeforeEach
    void setUp() {
        household = Household.builder().id(HOUSEHOLD_ID).name("Flat").code(CODE).build();
        when(householdRepository.findByCode(CODE)).thenReturn(Optional.of(household));
    }

    private JoinHouseholdRequestDTO request() {
        JoinHouseholdRequestDTO r = new JoinHouseholdRequestDTO();
        r.setCode(CODE);
        return r;
    }

    @Test
    void reJoiningClearsTheTombstoneAndKeepsTheMemberId() {
        HouseholdMember tombstoned = HouseholdMember.builder()
                .id(MEMBER_ID)
                .userId(USER_ID)
                .role(HouseholdRole.ROLE_MEMBER)
                .household(household)
                .removedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        when(householdMemberRepository.findAnyByUserIdAndHousehold(USER_ID, household))
                .thenReturn(Optional.of(tombstoned));
        when(householdMemberRepository.save(tombstoned)).thenReturn(tombstoned);

        var response = service.joinHousehold(request(), USER_ID);

        assertThat(tombstoned.getRemovedAt()).isNull();
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(producer.published).containsExactly(
                new RecordingProducer.Published(tombstoned, household, "MEMBER_JOINED"));
    }

    /** An active member joining again must not produce a duplicate event. */
    @Test
    void joiningWhileAlreadyActiveIsANoOp() {
        HouseholdMember active = HouseholdMember.builder()
                .id(MEMBER_ID)
                .userId(USER_ID)
                .role(HouseholdRole.ROLE_MEMBER)
                .household(household)
                .build();
        when(householdMemberRepository.findAnyByUserIdAndHousehold(USER_ID, household))
                .thenReturn(Optional.of(active));

        service.joinHousehold(request(), USER_ID);

        verify(householdMemberRepository, never()).save(any());
        assertThat(producer.published).isEmpty();
    }

    @Test
    void aFirstTimeJoinStillInsertsAndPublishes() {
        when(householdMemberRepository.findAnyByUserIdAndHousehold(USER_ID, household))
                .thenReturn(Optional.empty());
        when(householdMemberRepository.save(any(HouseholdMember.class)))
                .thenAnswer(inv -> {
                    HouseholdMember m = inv.getArgument(0);
                    m.setId(MEMBER_ID);
                    return m;
                });

        var response = service.joinHousehold(request(), USER_ID);

        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(producer.published).hasSize(1);
    }

    /*
     * A recording subclass rather than @Mock: HouseholdMemberEventProducer is a
     * concrete class, and the inline mock maker cannot instrument those on JDK
     * 25 -- every test in this file errored before this was introduced. super
     * takes nulls because publish() is overridden and never reaches them.
     */
    static class RecordingProducer extends HouseholdMemberEventProducer {
        record Published(HouseholdMember member, Household household, String eventType) {}

        final List<Published> published = new ArrayList<>();

        RecordingProducer() {
            super(null, null, null);
        }

        @Override
        public void publish(HouseholdMember member, Household household, String eventType) {
            published.add(new Published(member, household, eventType));
        }
    }
}
