package com.medclinic.main.repository;

import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByEmployeeId(Long employeeId);

    List<Appointment> findByClientId(Long clientId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE " +
            "(:employeeId IS NULL OR a.employee.id = :employeeId) AND " +
            "(:clientId IS NULL OR a.client.id = :clientId) AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:fromDate IS NULL OR a.startTime >= :fromDate) AND " +
            "(:toDate IS NULL OR a.startTime <= :toDate)")
    Page<Appointment> findAllFiltered(
            @Param("employeeId") Long employeeId,
            @Param("clientId") Long clientId,
            @Param("status") AppointmentStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT a FROM Appointment a WHERE a.employee.id = :employeeId " +
            "AND a.status <> 'CANCELLED' " +
            "AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
