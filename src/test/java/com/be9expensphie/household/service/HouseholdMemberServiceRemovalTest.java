package com.be9expensphie.household.service;

import com.be9expensphie.household.entity.Household;
import com.be9expensphie.household.entity.HouseholdMember;
import com.be9expensphie.common.enums.HouseholdRole;
import com.be9expensphie.household.exception.ConflictException;
import com.be9expensphie.household.exception.ForbiddenException;
import com.be9expensphie.household.exception.NotFoundException;
import com.be9expensphie.household.producer.HouseholdMemberEventProducer;
import com.be9expensphie.household.repository.HouseholdMemberRepository;
import com.be9expensphie.household.repository.HouseholdRepository;
import com.be9expensphie.household.repository.UserSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdMemberServiceRemovalTest {

    private static final long HOUSEHOLD_ID = 3L;
    private static final long ADMIN_USER_ID = 1L;
    private static final long MEMBER_USER_ID = 2L;
    private static final long ADMIN_MEMBER_ID = 10L;
    private static final long MEMBER_MEMBER_ID = 20L;

    @Mock private HouseholdMemberRepository memberRepo;
    @Mock private HouseholdRepository householdRepo;
    @Mock private UserSummaryRepository userSummaryRepository;
    private final RecordingProducer producer = new RecordingProducer();

    @InjectMocks private HouseholdMemberService service;

    /*
     * @InjectMocks populates @Mock fields only, and producer is a hand-written
     * recorder rather than a mock, so it has to be set here.
     */
    @BeforeEach
    void injectRecordingProducer() {
        ReflectionTestUtils.setField(service, "householdMemberEventProducer", producer);
    }

    private static final Household HOUSEHOLD = Household.builder().id(HOUSEHOLD_ID).build();

    private static HouseholdMember member(Long id, Long userId, HouseholdRole role) {
        return HouseholdMember.builder()
                .id(id)
                .userId(userId)
                .role(role)
                .household(HOUSEHOLD)
                .build();
    }

    private void requesterIs(HouseholdMember m) {
        when(memberRepo.findByUserIdAndHouseholdIdAndRemovedAtIsNull(m.getUserId(), HOUSEHOLD_ID))
                .thenReturn(Optional.of(m));
    }

    private void targetIs(HouseholdMember m) {
        when(memberRepo.findByIdAndHouseholdIdAndRemovedAtIsNull(m.getId(), HOUSEHOLD_ID))
                .thenReturn(Optional.of(m));
    }

    private void adminCountIs(long count) {
        when(memberRepo.countByHouseholdIdAndRoleAndRemovedAtIsNull(HOUSEHOLD_ID, HouseholdRole.ROLE_ADMIN))
                .thenReturn(count);
    }

    @Test
    void adminCanRemoveAnotherMember() {
        HouseholdMember target = member(MEMBER_MEMBER_ID, MEMBER_USER_ID, HouseholdRole.ROLE_MEMBER);
        requesterIs(member(ADMIN_MEMBER_ID, ADMIN_USER_ID, HouseholdRole.ROLE_ADMIN));
        targetIs(target);

        service.removeMember(HOUSEHOLD_ID, MEMBER_MEMBER_ID, ADMIN_USER_ID);

        assertThat(target.getRemovedAt()).isNotNull();
        verify(memberRepo).save(target);
        assertThat(producer.published).containsExactly(
                new RecordingProducer.Published(target, HOUSEHOLD, "MEMBER_LEFT"));
    }

    @Test
    void aMemberCanLeaveOnTheirOwn() {
        HouseholdMember self = member(MEMBER_MEMBER_ID, MEMBER_USER_ID, HouseholdRole.ROLE_MEMBER);
        requesterIs(self);
        targetIs(self);

        service.removeMember(HOUSEHOLD_ID, MEMBER_MEMBER_ID, MEMBER_USER_ID);

        assertThat(self.getRemovedAt()).isNotNull();
        assertThat(producer.published).containsExactly(
                new RecordingProducer.Published(self, HOUSEHOLD, "MEMBER_LEFT"));
    }

    @Test
    void aPlainMemberCannotRemoveSomeoneElse() {
        requesterIs(member(MEMBER_MEMBER_ID, MEMBER_USER_ID, HouseholdRole.ROLE_MEMBER));
        targetIs(member(ADMIN_MEMBER_ID, ADMIN_USER_ID, HouseholdRole.ROLE_ADMIN));

        assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, ADMIN_MEMBER_ID, MEMBER_USER_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only admin can remove another member");

        verify(memberRepo, never()).save(any());
        assertThat(producer.published).isEmpty();
    }

    /** Without an admin, expense-service cannot resolve a reviewer and all expense creation fails. */
    @Test
    void theLastAdminCannotBeRemoved() {
        requesterIs(member(ADMIN_MEMBER_ID, ADMIN_USER_ID, HouseholdRole.ROLE_ADMIN));
        targetIs(member(ADMIN_MEMBER_ID, ADMIN_USER_ID, HouseholdRole.ROLE_ADMIN));
        adminCountIs(1);

        assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, ADMIN_MEMBER_ID, ADMIN_USER_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot remove the last admin of this household");

        verify(memberRepo, never()).save(any());
        assertThat(producer.published).isEmpty();
    }

    /** The guard is about the last admin, not about admins generally. */
    @Test
    void anAdminCanBeRemovedWhileAnotherRemains() {
        HouseholdMember target = member(ADMIN_MEMBER_ID, ADMIN_USER_ID, HouseholdRole.ROLE_ADMIN);
        requesterIs(target);
        targetIs(target);
        adminCountIs(2);

        service.removeMember(HOUSEHOLD_ID, ADMIN_MEMBER_ID, ADMIN_USER_ID);

        assertThat(target.getRemovedAt()).isNotNull();
        assertThat(producer.published).containsExactly(
                new RecordingProducer.Published(target, HOUSEHOLD, "MEMBER_LEFT"));
    }

    @Test
    void aNonMemberCannotRemoveAnyone() {
        when(memberRepo.findByUserIdAndHouseholdIdAndRemovedAtIsNull(anyLong(), eq(HOUSEHOLD_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, MEMBER_MEMBER_ID, 999L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Not a member of this household");

        verify(memberRepo, never()).save(any());
    }

    /** An already-removed member is not found by the filtered lookup, so a repeat call 404s. */
    @Test
    void removingAnUnknownOrAlreadyRemovedMemberIsNotFound() {
        requesterIs(member(ADMIN_MEMBER_ID, ADMIN_USER_ID, HouseholdRole.ROLE_ADMIN));
        when(memberRepo.findByIdAndHouseholdIdAndRemovedAtIsNull(MEMBER_MEMBER_ID, HOUSEHOLD_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeMember(HOUSEHOLD_ID, MEMBER_MEMBER_ID, ADMIN_USER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Member not found in this household");

        verify(memberRepo, never()).save(any());
        assertThat(producer.published).isEmpty();
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
