package com.medclinic.main.dto;

public record UpdateClientRequest(
        String firstName,
        String lastName,
        String phone,
        String email,
        String notes
) {}
