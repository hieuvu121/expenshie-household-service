package com.be9expensphie.household.controller;

import com.be9expensphie.household.dto.CreateHouseholdDTO.CreateRequest;
import com.be9expensphie.household.dto.CreateHouseholdDTO.CreateResponse;
import com.be9expensphie.household.dto.HouseholdDTO;
import com.be9expensphie.household.dto.JoinHouseholdDTO.JoinHouseholdRequestDTO;
import com.be9expensphie.household.dto.JoinHouseholdDTO.JoinHouseholdResponseDTO;
import com.be9expensphie.household.service.HouseholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;

    @PostMapping("/create")
    public ResponseEntity<CreateResponse> createHousehold(
            @Valid @RequestBody CreateRequest request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(householdService.create(request, userId));
    }

    @PostMapping("/join")
    public ResponseEntity<JoinHouseholdResponseDTO> joinHousehold(
            @Valid @RequestBody JoinHouseholdRequestDTO request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(householdService.joinHousehold(request, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<HouseholdDTO>> getHousehold(
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(householdService.getHousehold(userId));
    }
}
