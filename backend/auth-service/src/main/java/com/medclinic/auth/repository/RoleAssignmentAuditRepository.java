package com.medclinic.auth.repository;

import com.medclinic.auth.model.RoleAssignmentAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleAssignmentAuditRepository extends JpaRepository<RoleAssignmentAudit, Long> {
}
