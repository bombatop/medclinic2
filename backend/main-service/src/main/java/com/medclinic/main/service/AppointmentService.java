package com.medclinic.main.service;

import com.medclinic.main.dto.AppointmentResponse;
import com.medclinic.main.dto.CreateAppointmentRequest;
import com.medclinic.main.dto.UpdateAppointmentRequest;
import com.medclinic.main.event.AppointmentEventPublisher;
import com.medclinic.main.exception.ConflictException;
import com.medclinic.main.exception.ResourceNotFoundException;
import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.model.Client;
import com.medclinic.main.model.Employee;
import com.medclinic.main.repository.AppointmentRepository;
import com.medclinic.main.repository.AppointmentSpecification;
import com.medclinic.main.repository.ClientRepository;
import com.medclinic.main.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final AppointmentEventPublisher eventPublisher;

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!employee.isActive()) {
            throw new ConflictException("Employee is not active");
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<Appointment> overlapping = appointmentRepository.findOverlapping(
                employee.getId(), request.startTime(), request.endTime());

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Employee already has an appointment in this time slot");
        }

        Appointment appointment = Appointment.builder()
                .employee(employee)
                .client(client)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .notes(request.notes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publish(saved, "CREATED");
        return AppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(AppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAllAppointmentsFiltered(
            Long employeeId,
            Long clientId,
            AppointmentStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        var spec = AppointmentSpecification.withFilters(employeeId, clientId, status, fromDate, toDate);
        return appointmentRepository.findAll(spec, pageable).map(AppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByEmployee(Long employeeId) {
        return appointmentRepository.findByEmployeeId(employeeId).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByClient(Long clientId) {
        return appointmentRepository.findByClientId(clientId).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .map(AppointmentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publish(saved, "STATUS_" + status.name());
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse updateAppointment(Long id, UpdateAppointmentRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        LocalDateTime previousStart = appointment.getStartTime();
        Long previousClientId = appointment.getClient().getId();

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!employee.isActive()) {
            throw new ConflictException("Employee is not active");
        }

        Client client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<Appointment> overlapping = appointmentRepository.findOverlapping(
                employee.getId(), request.startTime(), request.endTime());
        overlapping.removeIf(a -> a.getId().equals(id));

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Employee already has an appointment in this time slot");
        }

        appointment.setEmployee(employee);
        appointment.setClient(client);
        appointment.setStartTime(request.startTime());
        appointment.setEndTime(request.endTime());
        appointment.setNotes(request.notes());

        if (!previousStart.equals(request.startTime()) || !previousClientId.equals(client.getId())) {
            appointment.setReminder24hSentAt(null);
            appointment.setReminder1hSentAt(null);
        }

        Appointment saved = appointmentRepository.save(appointment);
        eventPublisher.publish(saved, "UPDATED");
        return AppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Long getEmployeeAuthUserId(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .map(Employee::getAuthUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    @Transactional(readOnly = true)
    public Long getAppointmentEmployeeAuthUserId(Long appointmentId) {
        return appointmentRepository.findByIdWithEmployee(appointmentId)
                .map(appointment -> appointment.getEmployee().getAuthUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }

    @Transactional(readOnly = true)
    public Long getAppointmentEmployeeId(Long appointmentId) {
        return appointmentRepository.findByIdWithEmployee(appointmentId)
                .map(appointment -> appointment.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
    }
}
