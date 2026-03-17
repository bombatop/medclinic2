package com.medclinic.main.controller;

import com.medclinic.main.dto.CreateEmployeeRequest;
import com.medclinic.main.dto.EmployeeResponse;
import com.medclinic.main.dto.PageResponse;
import com.medclinic.main.dto.UpdateEmployeeRequest;
import com.medclinic.main.exception.AccessDeniedException;
import com.medclinic.main.security.Permissions;
import com.medclinic.main.security.RequestContext;
import com.medclinic.main.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final RequestContext requestContext;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        requireEmployeeManagePermission();
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> getAllEmployees(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(employeeService.getAllEmployees(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyProfile() {
        return ResponseEntity.ok(employeeService.getEmployeeByAuthUserId(requestContext.getUserId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateEmployeeRequest request) {
        requireEmployeeManagePermission();
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        requireEmployeeManagePermission();
        employeeService.deactivateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateEmployee(@PathVariable Long id) {
        requireEmployeeManagePermission();
        employeeService.activateEmployee(id);
        return ResponseEntity.noContent().build();
    }

    private void requireEmployeeManagePermission() {
        if (!requestContext.hasPermission(Permissions.EMPLOYEE_MANAGE)) {
            throw new AccessDeniedException("Permission employee.manage is required");
        }
    }
}
