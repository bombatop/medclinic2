package com.medclinic.shared.event;

import java.time.Instant;

/**
 * RBAC / user admin actions for ops notifications. JSON field names are camelCase.
 */
public record AdminSecurityEvent(
        /** e.g. USER_CREATED, USER_ROLES_UPDATED, ROLE_PERMISSIONS_UPDATED */
        String eventType,
        String actorUsername,
        /** Target user username when applicable */
        String targetUsername,
        /** Role code when applicable */
        String roleCode,
        /** Short human-readable summary */
        String summary,
        Instant occurredAt
) {}
