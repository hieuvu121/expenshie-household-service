package com.be9expensphie.household.dto.CreateHouseholdDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String name;
}
