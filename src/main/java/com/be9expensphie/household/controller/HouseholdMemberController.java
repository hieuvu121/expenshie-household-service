package com.be9expensphie.household.controller;

import com.be9expensphie.household.dto.MemberDTO;
import com.be9expensphie.household.service.HouseholdMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/households")
@RequiredArgsConstructor
public class HouseholdMemberController {

    private final HouseholdMemberService memberService;

    @GetMapping("/{householdId}/members")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable Long householdId) {
        return ResponseEntity.ok(memberService.getMembers(householdId));
    }
}
