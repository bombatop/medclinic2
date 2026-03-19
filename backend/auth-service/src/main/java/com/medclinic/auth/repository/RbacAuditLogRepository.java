package com.medclinic.auth.repository;

import com.medclinic.auth.model.RbacAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RbacAuditLogRepository extends JpaRepository<RbacAuditLog, Long> {
}
