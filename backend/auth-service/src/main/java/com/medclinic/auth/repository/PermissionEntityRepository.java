package com.medclinic.auth.repository;

import com.medclinic.auth.model.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionEntityRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByCode(String code);

    List<PermissionEntity> findByCodeIn(Collection<String> codes);
}
