package com.be9expensphie.household.dto.JoinHouseholdDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class JoinHouseholdResponseDTO {
    private Long householdId;
    private String householdName;
    private String role;
    private Long memberId;
}
