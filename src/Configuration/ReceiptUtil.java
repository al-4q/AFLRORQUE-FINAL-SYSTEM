package Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Creates and manages receipts. A receipt is created when a booking status is set to "Arrived".
 */
public final class ReceiptUtil {

    private ReceiptUtil() { }

    /**
     * Creates a receipt for the given booking if it does not already exist.
     * Fetches booking details and user info, then inserts into receipts.
     * Call this when booking status is updated to "Arrived".
     *
     * @param conn active connection (caller must close)
     * @param bId  booking id
     * @return true if receipt was created, false if already exists or booking not found
     */
    public static boolean createReceiptForBooking(Connection conn, int bId) throws SQLException {
        if (conn == null) return false;
        // Check if receipt already exists for this booking
        try (PreparedStatement check = conn.prepareStatement("SELECT r_id FROM receipts WHERE b_id = ?")) {
            check.setInt(1, bId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return false;
            }
        }
        Integer uId = null;
        String username = null;
        String route = null;
        String seat = null;
        String date = null;
        Integer vId = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT passenger_id, passenger, route, seat, date, v_id FROM bookings WHERE b_id = ?")) {
            ps.setInt(1, bId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                uId = (Integer) rs.getObject("passenger_id");
                username = rs.getString("passenger");
                route = rs.getString("route");
                seat = rs.getString("seat");
                date = rs.getString("date");
                vId = (Integer) rs.getObject("v_id");
            }
        }
        if (uId != null) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE u_id = ?")) {
                ps.setInt(1, uId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) username = rs.getString("username");
                }
            }
        }
        if (username == null) username = "";
        String origin = "";
        String destination = "";
        if (vId != null) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT origin, destination FROM routes WHERE v_id = ?")) {
                ps.setInt(1, vId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        origin = nullToEmpty(rs.getString("origin"));
                        destination = nullToEmpty(rs.getString("destination"));
                    }
                }
            }
        }
        if (origin.isEmpty() && destination.isEmpty() && route != null && !route.isEmpty()) {
            int dash = route.indexOf(" - ");
            if (dash > 0) {
                origin = route.substring(0, dash).trim();
                destination = route.substring(dash + 3).trim();
            } else {
                origin = route;
            }
        }
        int nextRid;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(MAX(r_id), 0) + 1 AS next_id FROM receipts");
             ResultSet rs = ps.executeQuery()) {
            nextRid = rs.next() ? rs.getInt("next_id") : 1;
        }
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO receipts (r_id, u_id, username, b_id, origin, destination, seat, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ins.setInt(1, nextRid);
            ins.setObject(2, uId);
            ins.setString(3, username);
            ins.setInt(4, bId);
            ins.setString(5, origin);
            ins.setString(6, destination);
            ins.setString(7, seat);
            ins.setString(8, date);
            ins.executeUpdate();
        }
        return true;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
