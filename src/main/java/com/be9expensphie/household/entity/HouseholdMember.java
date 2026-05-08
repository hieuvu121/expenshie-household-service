package com.be9expensphie.household.entity;

import com.be9expensphie.household.enums.HouseholdRole;
import jakarta.persistence.*;
import lombok.*;

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
}
