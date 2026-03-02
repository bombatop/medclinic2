package com.medclinic.main.repository;

import com.medclinic.main.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByAuthUserId(Long authUserId);

    boolean existsByAuthUserId(Long authUserId);
}
