package Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One logical vehicle per plate: reuse {@code vehicles} rows, one Active session per plate,
 * and plate stays tied to the same customer (owner_name) after first use—even when past sessions are Completed.
 */
public final class VehiclePlateUtil {

    private VehiclePlateUtil() { }

    public static final class PlateOwnerConflictException extends Exception {
        public PlateOwnerConflictException(String message) {
            super(message);
        }
    }

    private static boolean sameOwner(String a, String b) {
        String na = a == null ? "" : a.trim();
        String nb = b == null ? "" : b.trim();
        return na.equalsIgnoreCase(nb);
    }

    public static int countActiveParkingForPlate(Connection conn, String normalizedPlate) throws SQLException {
        if (normalizedPlate == null || normalizedPlate.isEmpty()) {
            return 0;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM parking_transactions pt "
                        + "INNER JOIN vehicles v ON pt.vehicle_id = v.vehicle_id "
                        + "WHERE upper(trim(v.plate_number)) = ? AND lower(trim(pt.status)) = 'active'")) {
            ps.setString(1, normalizedPlate.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Returns {@code vehicle_id} for this plate: updates existing row (same plate, any casing) or inserts new.
     * If the plate already exists for a different {@code owner_name}, throws {@link PlateOwnerConflictException}.
     */
    public static int findOrCreateVehicle(Connection conn, String normalizedPlate, String vehicleType, String ownerName)
            throws SQLException, PlateOwnerConflictException {
        String plate = normalizedPlate.trim().toUpperCase();
        try (PreparedStatement find = conn.prepareStatement(
                "SELECT vehicle_id, owner_name FROM vehicles WHERE upper(trim(plate_number)) = ? ORDER BY vehicle_id LIMIT 1")) {
            find.setString(1, plate);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    int vid = rs.getInt(1);
                    String existingOwner = rs.getString(2);
                    if (!sameOwner(existingOwner, ownerName)) {
                        throw new PlateOwnerConflictException(
                                "This plate number is already registered to another customer. "
                                        + "It cannot be used by a different person, even after their parking is completed.");
                    }
                    try (PreparedStatement up = conn.prepareStatement(
                            "UPDATE vehicles SET plate_number = ?, vehicle_type = ?, owner_name = ? WHERE vehicle_id = ?")) {
                        up.setString(1, plate);
                        up.setString(2, vehicleType);
                        up.setString(3, ownerName);
                        up.setInt(4, vid);
                        up.executeUpdate();
                    }
                    return vid;
                }
            }
        }
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO vehicles (plate_number, vehicle_type, owner_name) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, plate);
            ins.setString(2, vehicleType);
            ins.setString(3, ownerName);
            ins.executeUpdate();
            try (ResultSet keys = ins.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to get vehicle id");
                }
                return keys.getInt(1);
            }
        }
    }
}
