package com.be9expensphie.household.dto.JoinHouseholdDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinHouseholdRequestDTO {
    @NotBlank
    private String code;
}
