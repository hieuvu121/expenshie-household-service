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
    public ResponseEntity<List<MemberDTO>> getMembers(
            @PathVariable Long householdId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(memberService.getMembers(householdId, userId));
    }

    /**
     * Removes a member, or leaves the household when memberId is the caller's
     * own membership — the service authorizes both against the same path.
     *
     * X-User-Id is injected by the gateway's JwtAuthenticationFilter and is
     * never accepted from the client, so it identifies the requester; memberId
     * identifies the membership being retired, which is not the same thing as
     * a user id.
     */
    @DeleteMapping("/{householdId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long householdId,
            @PathVariable Long memberId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        memberService.removeMember(householdId, memberId, userId);
        return ResponseEntity.noContent().build();
    }
}
