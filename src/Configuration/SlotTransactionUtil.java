package Configuration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps {@code parking_slots.status} aligned with {@code parking_transactions}:
 * a slot must not be treated as empty for parking while an Active transaction exists.
 */
public final class SlotTransactionUtil {

    private SlotTransactionUtil() { }

    public static int countActiveTransactionsOnSlot(Connection conn, int slotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM parking_transactions WHERE slot_id = ? AND lower(trim(status)) = 'active'")) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * SQL fragment: parking_slots rows that are Available and have no Active parking_transactions.
     * Use as: SELECT ... FROM parking_slots ps WHERE ... AND " + NO_ACTIVE_PARKING_SUBQUERY
     */
    public static final String NO_ACTIVE_PARKING_ON_SLOT =
            "NOT EXISTS (SELECT 1 FROM parking_transactions pt "
                    + "WHERE pt.slot_id = ps.slot_id AND lower(trim(pt.status)) = 'active')";
}
