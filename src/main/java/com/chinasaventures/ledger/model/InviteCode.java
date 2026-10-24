package com.chinasaventures.ledger.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "invite_codes")
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // CHANGED
    private Long id;

    @Column(name = "code", nullable = false, unique = true) // CHANGED: renamed from inviteCode, now unique
    private String code;

    @Column(nullable = false) // CHANGED: role is trusted for authorization, never null
    private String role; // 'worker' or 'director'; String leaves room for future roles

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Users createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false) // CHANGED: removed @CreationTimestamp, set in service
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private Users usedBy;

    @Column(name = "used_at") // CHANGED: removed @ManyToOne/@JoinColumn, it's a plain timestamp
    private LocalDateTime usedAt;
}