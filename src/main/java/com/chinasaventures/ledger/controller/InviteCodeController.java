package com.chinasaventures.ledger.controller;

import com.chinasaventures.ledger.dto.CreateInviteRequest;
import com.chinasaventures.ledger.dto.InviteCodeResponseDTO;
import com.chinasaventures.ledger.service.InviteCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController // NEW
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteCodeController {

    private final InviteCodeService inviteCodeService;

    @PostMapping
    public ResponseEntity<InviteCodeResponseDTO> generate(@RequestBody CreateInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inviteCodeService.generateCode(request.role()));
    }
}