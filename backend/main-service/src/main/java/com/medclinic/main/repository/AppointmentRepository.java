package com.medclinic.main.repository;

import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByEmployeeId(Long employeeId);

    List<Appointment> findByClientId(Long clientId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.employee.id = :employeeId " +
            "AND a.status <> 'CANCELLED' " +
            "AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
