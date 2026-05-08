package com.be9expensphie.household.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdDTO {
    private Long id;
    private String name;
    private String code;
    private String role;
    private Long memberId;
}
