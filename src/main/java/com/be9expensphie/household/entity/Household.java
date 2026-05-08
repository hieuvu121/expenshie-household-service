package com.be9expensphie.household.entity;

import com.be9expensphie.household.enums.HouseholdRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "household")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Household {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private Long createdBy;

    @OneToMany(mappedBy = "household", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HouseholdMember> members;
}
