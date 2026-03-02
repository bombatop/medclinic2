package com.medclinic.main.dto;

public record UpdateEmployeeRequest(
        String firstName,
        String lastName,
        String specialization
) {}
