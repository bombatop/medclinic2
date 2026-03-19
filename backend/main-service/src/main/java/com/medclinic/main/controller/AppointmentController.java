package com.medclinic.main.controller;

import com.medclinic.main.dto.AppointmentResponse;
import com.medclinic.main.dto.CreateAppointmentRequest;
import com.medclinic.main.dto.UpdateAppointmentRequest;
import com.medclinic.main.dto.PageResponse;
import com.medclinic.main.exception.AccessDeniedException;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.security.Permissions;
import com.medclinic.main.security.RequestContext;
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
    private final RequestContext requestContext;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        requireCanCreateAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> updateAppointment(@PathVariable Long id,
                                                                 @Valid @RequestBody UpdateAppointmentRequest request) {
        requireCanUpdateAppointment(id, request);
        return ResponseEntity.ok(appointmentService.updateAppointment(id, request));
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
        requireCanUpdateStatus(id, status);
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }

    private void requireCanCreateAppointment(CreateAppointmentRequest request) {
        if (requestContext.hasPermission(Permissions.APPOINTMENT_CREATE_ANY)) {
            return;
        }
        if (!requestContext.hasPermission(Permissions.APPOINTMENT_CREATE_SELF)
                || !requestContext.hasPermission(Permissions.APPOINTMENT_PARTICIPATE)) {
            throw new AccessDeniedException("No permission to create appointments");
        }
        Long ownerUserId = appointmentService.getEmployeeAuthUserId(request.employeeId());
        if (!ownerUserId.equals(requestContext.getUserId())) {
            throw new AccessDeniedException("You can create appointments only for your own doctor profile");
        }
    }

    private void requireCanUpdateAppointment(Long id, UpdateAppointmentRequest request) {
        if (requestContext.hasPermission(Permissions.APPOINTMENT_UPDATE_ANY)) {
            return;
        }
        if (!requestContext.hasPermission(Permissions.APPOINTMENT_UPDATE_SELF)
                || !requestContext.hasPermission(Permissions.APPOINTMENT_PARTICIPATE)) {
            throw new AccessDeniedException("No permission to update appointments");
        }
        Long currentOwnerUserId = appointmentService.getAppointmentEmployeeAuthUserId(id);
        Long targetOwnerUserId = appointmentService.getEmployeeAuthUserId(request.employeeId());
        Long currentUserId = requestContext.getUserId();
        if (!currentOwnerUserId.equals(currentUserId) || !targetOwnerUserId.equals(currentUserId)) {
            throw new AccessDeniedException("You can update only your own appointments");
        }
    }

    private void requireCanUpdateStatus(Long id, AppointmentStatus status) {
        if (status == AppointmentStatus.CANCELLED) {
            if (requestContext.hasPermission(Permissions.APPOINTMENT_CANCEL_ANY)) {
                return;
            }
            if (!requestContext.hasPermission(Permissions.APPOINTMENT_CANCEL_SELF)
                    || !requestContext.hasPermission(Permissions.APPOINTMENT_PARTICIPATE)) {
                throw new AccessDeniedException("No permission to cancel appointments");
            }
        } else {
            if (requestContext.hasPermission(Permissions.APPOINTMENT_STATUS_UPDATE_ANY)) {
                return;
            }
            if (!requestContext.hasPermission(Permissions.APPOINTMENT_STATUS_UPDATE_SELF)
                    || !requestContext.hasPermission(Permissions.APPOINTMENT_PARTICIPATE)) {
                throw new AccessDeniedException("No permission to update appointment status");
            }
        }

        Long ownerUserId = appointmentService.getAppointmentEmployeeAuthUserId(id);
        if (!ownerUserId.equals(requestContext.getUserId())) {
            throw new AccessDeniedException("You can change status only for your own appointments");
        }
    }
}
