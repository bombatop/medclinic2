package com.medclinic.main.dto;

import com.medclinic.main.model.Employee;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        Long authUserId,
        String firstName,
        String lastName,
        String specialization,
        boolean active,
        Instant createdAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getAuthUserId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getSpecialization(),
                employee.isActive(),
                employee.getCreatedAt()
        );
    }
}
