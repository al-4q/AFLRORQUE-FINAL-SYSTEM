package Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Reservation logic helpers: expiration, validation, slot lookup.
 */
public final class ReservationUtil {

    public static final int VALIDITY_MINUTES = 30;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ReservationUtil() {}

    /** Expires old Active reservations and frees their slots. Call on load. */
    public static void expireOldReservations(Connection conn) throws SQLException {
        String now = LocalDateTime.now().format(ISO);
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT reservation_id, slot_id FROM reservations WHERE status = 'Active' AND valid_until IS NOT NULL AND valid_until < ?")) {
            sel.setString(1, now);
            try (ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    int rid = rs.getInt("reservation_id");
                    int sid = rs.getInt("slot_id");
                    try (PreparedStatement up = conn.prepareStatement("UPDATE reservations SET status = 'Expired' WHERE reservation_id = ?")) {
                        up.setInt(1, rid);
                        up.executeUpdate();
                    }
                    try (PreparedStatement up = conn.prepareStatement("UPDATE parking_slots SET status = 'Available' WHERE slot_id = ?")) {
                        up.setInt(1, sid);
                        up.executeUpdate();
                    }
                }
            }
        }
    }

    /** Returns valid_until timestamp (now + VALIDITY_MINUTES). */
    public static String computeValidUntil() {
        return LocalDateTime.now().plus(Duration.ofMinutes(VALIDITY_MINUTES)).format(ISO);
    }

    /** Returns user's active reservation slot_id, or -1 if none. */
    public static int getActiveReservationSlotId(Connection conn, int userId) throws SQLException {
        String now = LocalDateTime.now().format(ISO);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_id FROM reservations WHERE user_id = ? AND status = 'Active' " +
                "AND (valid_until IS NULL OR valid_until >= ?) ORDER BY reservation_id DESC LIMIT 1")) {
            ps.setInt(1, userId);
            ps.setString(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("slot_id") : -1;
            }
        }
    }

    /** Returns reservation_id for user's active reservation, or -1. */
    public static int getActiveReservationId(Connection conn, int userId) throws SQLException {
        String now = LocalDateTime.now().format(ISO);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT reservation_id FROM reservations WHERE user_id = ? AND status = 'Active' " +
                "AND (valid_until IS NULL OR valid_until >= ?) ORDER BY reservation_id DESC LIMIT 1")) {
            ps.setInt(1, userId);
            ps.setString(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("reservation_id") : -1;
            }
        }
    }

    /** Count of active reservations for user. */
    public static int countActiveReservations(Connection conn, int userId) throws SQLException {
        String now = LocalDateTime.now().format(ISO);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM reservations WHERE user_id = ? AND status = 'Active' " +
                "AND (valid_until IS NULL OR valid_until >= ?)")) {
            ps.setInt(1, userId);
            ps.setString(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
