package com.medclinic.main.security;

public final class Permissions {
    private Permissions() {}

    public static final String EMPLOYEE_MANAGE = "employee.manage";

    public static final String APPOINTMENT_CREATE_ANY = "appointment.create_any";
    public static final String APPOINTMENT_UPDATE_ANY = "appointment.update_any";
    public static final String APPOINTMENT_STATUS_UPDATE_ANY = "appointment.status_update_any";
    public static final String APPOINTMENT_CANCEL_ANY = "appointment.cancel_any";
    public static final String APPOINTMENT_CREATE_SELF = "appointment.create_self";
    public static final String APPOINTMENT_UPDATE_SELF = "appointment.update_self";
    public static final String APPOINTMENT_STATUS_UPDATE_SELF = "appointment.status_update_self";
    public static final String APPOINTMENT_CANCEL_SELF = "appointment.cancel_self";
    public static final String APPOINTMENT_PARTICIPATE = "appointment.participate";
}
