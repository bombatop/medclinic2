package com.medclinic.main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "medclinic.reminders")
public class ReminderProperties {

    /**
     * Enabled reminder windows, e.g. H24, H1 (case-insensitive).
     */
    private List<String> windows = new ArrayList<>(List.of("H24", "H1"));

    /** Half-width of the time window around the target offset (e.g. 24h ± this many minutes). */
    private int windowHalfWidthMinutes = 6;

    /** How often the scheduler runs (ms). */
    private long pollIntervalMs = 120_000L;
}
