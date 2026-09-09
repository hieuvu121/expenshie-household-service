package com.be9expensphie.household.entity;

import com.be9expensphie.household.enums.HouseholdRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "household_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"household_id", "user_id"}),
    indexes = @Index(name = "idx_user_id", columnList = "user_id")
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "household")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HouseholdMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HouseholdRole role;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    /**
     * Set when the member is removed or leaves; null means an active member.
     *
     * Soft rather than hard delete so the member id survives. Settlements and
     * expenses in the other services reference this id, so a member who leaves
     * and re-joins keeps their outstanding balances and their expense history
     * instead of returning as a stranger.
     *
     * It is also forced by the schema: the (household_id, user_id) unique
     * constraint above would reject a re-join insert while a row for that pair
     * still exists, so joinHousehold() clears this field rather than inserting.
     *
     * Every membership query except that re-join lookup must filter on
     * removedAt is null.
     */
    @Column(name = "removed_at")
    private Instant removedAt;
}
