package com.medclinic.auth.model;

public enum Permission {
    USERS_READ_ALL("users.read_all"),
    USERS_MANAGE("users.manage"),
    USERS_MANAGE_ROLES("users.manage_roles"),

    EMPLOYEE_READ_ALL("employee.read_all"),
    EMPLOYEE_MANAGE("employee.manage"),

    APPOINTMENT_READ_ALL("appointment.read_all"),
    APPOINTMENT_CREATE_ANY("appointment.create_any"),
    APPOINTMENT_UPDATE_ANY("appointment.update_any"),
    APPOINTMENT_STATUS_UPDATE_ANY("appointment.status_update_any"),
    APPOINTMENT_CANCEL_ANY("appointment.cancel_any"),
    APPOINTMENT_CREATE_SELF("appointment.create_self"),
    APPOINTMENT_UPDATE_SELF("appointment.update_self"),
    APPOINTMENT_STATUS_UPDATE_SELF("appointment.status_update_self"),
    APPOINTMENT_CANCEL_SELF("appointment.cancel_self"),
    APPOINTMENT_PARTICIPATE("appointment.participate");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
