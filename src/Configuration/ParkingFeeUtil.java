package Configuration;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared parking fee calculation. Every second/minute counts as hourly rate.
 * Partial hours round up (e.g. 1 second = 1 hour).
 */
public final class ParkingFeeUtil {

    public static final double HOURLY_RATE = 70.0;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ParkingFeeUtil() {}

    /**
     * Calculates parking fee from time_in and time_out.
     * If time_out is null/empty, uses current time.
     * Every partial hour = 1 full hour (70 pesos).
     */
    public static double calculateFee(String timeInStr, String timeOutStr) {
        LocalDateTime timeIn;
        try {
            if (timeInStr == null || timeInStr.trim().isEmpty()) return 0;
            timeIn = LocalDateTime.parse(timeInStr.trim(), ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            return 0;
        }
        LocalDateTime timeOut;
        if (timeOutStr == null || timeOutStr.trim().isEmpty()) {
            timeOut = LocalDateTime.now();
        } else {
            try {
                timeOut = LocalDateTime.parse(timeOutStr.trim(), ISO_FORMATTER);
            } catch (DateTimeParseException e) {
                timeOut = LocalDateTime.now();
            }
        }
        if (!timeOut.isAfter(timeIn)) {
            return HOURLY_RATE; // minimum 1 hour
        }
        long seconds = Duration.between(timeIn, timeOut).getSeconds();
        long hours = (seconds + 3599) / 3600; // ceiling: any partial hour = 1 full hour
        return Math.max(1, hours) * HOURLY_RATE;
    }
}
