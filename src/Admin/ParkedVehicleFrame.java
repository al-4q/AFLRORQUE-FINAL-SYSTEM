package Admin;

import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Admin view: parking bays that are currently occupied, with who is parked (active session).
 */
public class ParkedVehicleFrame extends InternalPageFrame {

    private final DefaultTableModel tableModel;
    private final JTable table;

    public ParkedVehicleFrame() {
        super();
        tableModel = new DefaultTableModel(new Object[][]{}, new String[]{
                "Slot ID", "Slot Number", "Slot Type", "Status", "Parked by"
        }) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        AppTheme.styleTable(table);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(AppTheme.CONTENT_BG);
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        north.setBackground(AppTheme.CONTENT_BG);
        JLabel title = new JLabel("Parked vehicles (occupied slots only)");
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.FONT_TITLE);
        north.add(title);
        root.add(north, BorderLayout.NORTH);
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(root, BorderLayout.CENTER);
        loadOccupiedSlots();
    }

    private void loadOccupiedSlots() {
        tableModel.setRowCount(0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ps.slot_id, ps.slot_number, ps.slot_type, ps.status, "
                            + "(SELECT COALESCE(NULLIF(trim(v.owner_name), ''), '—') "
                            + "FROM parking_transactions pt "
                            + "INNER JOIN vehicles v ON v.vehicle_id = pt.vehicle_id "
                            + "WHERE pt.slot_id = ps.slot_id AND lower(trim(pt.status)) = 'active' "
                            + "ORDER BY pt.transaction_id DESC LIMIT 1) AS parked_by "
                            + "FROM parking_slots ps "
                            + "WHERE lower(trim(ps.status)) = ? "
                            + "ORDER BY ps.slot_id")) {
                ps.setString(1, "occupied");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String parkedBy = rs.getString("parked_by");
                        if (parkedBy == null || parkedBy.isEmpty()) {
                            parkedBy = "—";
                        }
                        tableModel.addRow(new Object[]{
                                rs.getInt("slot_id"),
                                rs.getString("slot_number"),
                                rs.getString("slot_type"),
                                rs.getString("status"),
                                parkedBy
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load parked slots: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }
}
