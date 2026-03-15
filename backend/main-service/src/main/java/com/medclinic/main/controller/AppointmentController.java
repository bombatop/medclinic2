package com.medclinic.main.controller;

import com.medclinic.main.dto.AppointmentResponse;
import com.medclinic.main.dto.CreateAppointmentRequest;
import com.medclinic.main.dto.PageResponse;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @GetMapping
    public ResponseEntity<PageResponse<AppointmentResponse>> getAllAppointments(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                appointmentService.getAllAppointmentsFiltered(employeeId, clientId, status, from, to, pageable)));
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
