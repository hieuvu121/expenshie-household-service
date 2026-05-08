package com.be9expensphie.household.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_summary")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String fullName;
}
