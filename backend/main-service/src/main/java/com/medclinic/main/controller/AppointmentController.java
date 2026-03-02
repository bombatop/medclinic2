package com.medclinic.main.controller;

import com.medclinic.main.dto.AppointmentResponse;
import com.medclinic.main.dto.CreateAppointmentRequest;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AppointmentResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByEmployee(employeeId));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<AppointmentResponse>> getByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByClient(clientId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(@PathVariable Long id,
                                                            @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }
}
