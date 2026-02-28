package com.medclinic.main.service;

import com.medclinic.main.dto.AppointmentResponse;
import com.medclinic.main.dto.CreateAppointmentRequest;
import com.medclinic.main.exception.ConflictException;
import com.medclinic.main.exception.ResourceNotFoundException;
import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;
import com.medclinic.main.model.Client;
import com.medclinic.main.model.Employee;
import com.medclinic.main.repository.AppointmentRepository;
import com.medclinic.main.repository.ClientRepository;
import com.medclinic.main.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;

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

        return AppointmentResponse.from(appointmentRepository.save(appointment));
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
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }
}
