package com.medclinic.main.repository;

import com.medclinic.main.model.Appointment;
import com.medclinic.main.model.AppointmentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AppointmentSpecification {

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> withFilters(
            Long employeeId,
            Long clientId,
            AppointmentStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {
        return (root, query, cb) -> {
            var pred = cb.conjunction();
            if (employeeId != null) {
                pred = cb.and(pred, cb.equal(root.get("employee").get("id"), employeeId));
            }
            if (clientId != null) {
                pred = cb.and(pred, cb.equal(root.get("client").get("id"), clientId));
            }
            if (status != null) {
                pred = cb.and(pred, cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                pred = cb.and(pred, cb.greaterThanOrEqualTo(root.get("startTime"), fromDate));
            }
            if (toDate != null) {
                pred = cb.and(pred, cb.lessThanOrEqualTo(root.get("startTime"), toDate));
            }
            return pred;
        };
    }
}
