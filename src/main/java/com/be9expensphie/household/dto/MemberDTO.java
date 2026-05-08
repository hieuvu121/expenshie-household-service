package com.be9expensphie.household.dto;

import com.be9expensphie.household.enums.HouseholdRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberDTO {
    private Long memberId;
    private String fullName;
    private HouseholdRole role;
}
