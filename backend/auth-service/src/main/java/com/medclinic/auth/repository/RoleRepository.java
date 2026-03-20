package com.medclinic.auth.repository;

import com.medclinic.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findByCode(String code);

    List<Role> findByCodeIn(Collection<String> codes);

    boolean existsByCode(String code);
}
