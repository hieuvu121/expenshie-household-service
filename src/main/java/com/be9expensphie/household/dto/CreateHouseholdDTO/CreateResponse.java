package com.be9expensphie.household.dto.CreateHouseholdDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateResponse {
    private Long id;
    private String name;
    private String role;
    private Long memberId;
}
