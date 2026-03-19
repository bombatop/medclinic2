package com.medclinic.auth.dto;

import com.medclinic.auth.model.RbacAuditLog;

import java.time.Instant;

public record RbacAuditLogResponse(
        Long id,
        Long actorUserId,
        String actorUsername,
        String action,
        String targetType,
        String targetRef,
        String details,
        Instant createdAt
) {
    public static RbacAuditLogResponse from(RbacAuditLog log) {
        return new RbacAuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getActorUsername(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetRef(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}
