package com.medclinic.main.model;

/**
 * Distinguishes 24h vs 1h reminder pipelines (time window offset + which sent-at column is used).
 */
public enum AppointmentReminderKind {

    H24(24, 0),
    H1(1, 1);

    private final int hoursBeforeVisit;
    /** Stable key for JPQL CASE/WHERE branches (do not reorder enum constants lightly). */
    private final int queryKey;

    AppointmentReminderKind(int hoursBeforeVisit, int queryKey) {
        this.hoursBeforeVisit = hoursBeforeVisit;
        this.queryKey = queryKey;
    }

    public int getHoursBeforeVisit() {
        return hoursBeforeVisit;
    }

    public int getQueryKey() {
        return queryKey;
    }

    /** Value passed to {@link com.medclinic.shared.event.AppointmentReminderEvent}. */
    public String eventType() {
        return name();
    }
}
