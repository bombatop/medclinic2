package com.medclinic.main.repository;

import com.medclinic.main.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);

    @Query("SELECT DISTINCT e.authUserId FROM Employee e")
    List<Long> findAllDistinctAuthUserIds();
}
