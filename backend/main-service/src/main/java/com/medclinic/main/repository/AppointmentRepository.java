package com.medclinic.main.repository;

import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByEmployeeId(Long employeeId);

    List<Appointment> findByClientId(Long clientId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.employee WHERE a.id = :id")
    Optional<Appointment> findByIdWithEmployee(@Param("id") Long id);

    @Query("SELECT a FROM Appointment a WHERE a.employee.id = :employeeId " +
            "AND a.status <> 'CANCELLED' " +
            "AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT a.id FROM Appointment a WHERE a.status = :status "
            + "AND a.startTime > :winStart AND a.startTime <= :winEnd "
            + "AND a.client.receiveAppointmentReminders = true AND a.client.telegramChatId IS NOT NULL "
            + "AND ((:kindKey = 0 AND a.reminder24hSentAt IS NULL) OR (:kindKey = 1 AND a.reminder1hSentAt IS NULL))")
    List<Long> findIdsEligibleForReminder(
            @Param("status") AppointmentStatus status,
            @Param("winStart") LocalDateTime winStart,
            @Param("winEnd") LocalDateTime winEnd,
            @Param("kindKey") int kindKey
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Appointment a SET "
            + "a.reminder24hSentAt = CASE WHEN :kindKey = 0 THEN :ts ELSE a.reminder24hSentAt END, "
            + "a.reminder1hSentAt = CASE WHEN :kindKey = 1 THEN :ts ELSE a.reminder1hSentAt END "
            + "WHERE a.id = :id "
            + "AND ((:kindKey = 0 AND a.reminder24hSentAt IS NULL) OR (:kindKey = 1 AND a.reminder1hSentAt IS NULL)) "
            + "AND a.status = :status AND a.startTime > :winStart AND a.startTime <= :winEnd "
            + "AND a.client.receiveAppointmentReminders = true AND a.client.telegramChatId IS NOT NULL")
    int claimReminder(
            @Param("id") Long id,
            @Param("ts") Instant ts,
            @Param("status") AppointmentStatus status,
            @Param("winStart") LocalDateTime winStart,
            @Param("winEnd") LocalDateTime winEnd,
            @Param("kindKey") int kindKey
    );

    @Query("SELECT a FROM Appointment a JOIN FETCH a.client JOIN FETCH a.employee WHERE a.id = :id")
    Optional<Appointment> findByIdForReminder(@Param("id") Long id);
}
