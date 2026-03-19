package com.medclinic.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "role_assignment_audit")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long actorUserId;

    @Column(nullable = false)
    private String actorUsername;

    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false, length = 512)
    private String rolesBefore;

    @Column(nullable = false, length = 512)
    private String rolesAfter;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
