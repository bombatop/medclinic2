package com.medclinic.auth.repository;

import com.medclinic.auth.model.Role;
import com.medclinic.auth.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleIn(Collection<Role> roles);

    List<RolePermission> findByRole(Role role);

    void deleteByRole(Role role);
}
