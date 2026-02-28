package com.medclinic.main.service;

import com.medclinic.main.dto.CreateEmployeeRequest;
import com.medclinic.main.dto.EmployeeResponse;
import com.medclinic.main.dto.UpdateEmployeeRequest;
import com.medclinic.main.exception.ConflictException;
import com.medclinic.main.exception.ResourceNotFoundException;
import com.medclinic.main.model.Employee;
import com.medclinic.main.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByAuthUserId(request.authUserId())) {
            throw new ConflictException("Employee profile already exists for this user");
        }

        Employee employee = Employee.builder()
                .authUserId(request.authUserId())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .specialization(request.specialization())
                .build();

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeByAuthUserId(Long authUserId) {
        return employeeRepository.findByAuthUserId(authUserId)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (request.firstName() != null) {
            employee.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            employee.setLastName(request.lastName());
        }
        if (request.specialization() != null) {
            employee.setSpecialization(request.specialization());
        }

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public void deactivateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    @Transactional
    public void activateEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        employee.setActive(true);
        employeeRepository.save(employee);
    }
}
