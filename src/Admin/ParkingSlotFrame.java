package Admin;

import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.SlotTransactionUtil;
import Configuration.StyledDialog;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Parking slot management: Add, Delete, Edit Slot, Slot Status.
 * Opened when admin clicks PARKINGSLOTPANEL in MainPage.
 */
public class ParkingSlotFrame extends InternalPageFrame {

    private final DefaultTableModel tableModel;
    private final List<Integer> userIds = new ArrayList<>();

    private javax.swing.JPanel AddSlotPanel;
    private javax.swing.JPanel DeleteSlotPanel;
    private javax.swing.JPanel EditSlotPanel;
    private javax.swing.JPanel ViewSlotStatusPanel;
    private javax.swing.JPanel ViewTransactionsPanel;
    private javax.swing.JPanel ReportsPanel;
    private javax.swing.JPanel DailySalesPanel;
    private javax.swing.JTable jTableBookings;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane jScrollPane1;

    public ParkingSlotFrame() {
        super();
        initComponents();
        tableModel = (DefaultTableModel) jTableBookings.getModel();
        tableModel.setColumnIdentifiers(new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"});
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTableBookings);
        makeContentFillFrame();
        setupPanelListeners();
        loadSlots();
    }

    private void makeContentFillFrame() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(0, 8));
        javax.swing.JPanel topPanel = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topPanel.setBackground(AppTheme.CONTENT_BG);
        topPanel.add(AddSlotPanel);
        topPanel.add(DeleteSlotPanel);
        topPanel.add(EditSlotPanel);
        topPanel.add(ViewSlotStatusPanel);
        topPanel.add(ViewTransactionsPanel);
        topPanel.add(ReportsPanel);
        topPanel.add(DailySalesPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(jScrollPane1, BorderLayout.CENTER);
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        AddSlotPanel.setCursor(handCursor);
        AddSlotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performAddSlot(); }
        });
        DeleteSlotPanel.setCursor(handCursor);
        DeleteSlotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performDeleteSlot(); }
        });
        EditSlotPanel.setCursor(handCursor);
        EditSlotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performEditSlot(); }
        });
        ViewSlotStatusPanel.setCursor(handCursor);
        ViewSlotStatusPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tableModel.setColumnIdentifiers(new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"});
                loadSlots();
            }
        });
        ViewTransactionsPanel.setCursor(handCursor);
        ViewTransactionsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                loadTransactions();
            }
        });
        ReportsPanel.setCursor(handCursor);
        ReportsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performReports(); }
        });
        DailySalesPanel.setCursor(handCursor);
        DailySalesPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performDailySales(); }
        });
        jTableBookings.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && jTableBookings.getSelectedRow() >= 0
                        && tableModel.getColumnCount() < 8) {
                    performEditSlot();
                }
            }
        });
    }

    private void performAddSlot() {
        JTextField numberField = new JTextField(15);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Car", "Motorcycle"});
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Slot number (e.g. A1, A2, B1):"));
        panel.add(numberField);
        panel.add(new javax.swing.JLabel("Slot type:"));
        panel.add(typeCombo);
        if (JOptionPane.showConfirmDialog(this, panel, "Add Parking Slot", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String slotNumber = numberField.getText().trim();
        if (slotNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Slot number is required.", "Add Slot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String slotType = (String) typeCombo.getSelectedItem();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO parking_slots (slot_number, slot_type, status) VALUES (?, ?, ?)")) {
                ps.setString(1, slotNumber);
                ps.setString(2, slotType);
                ps.setString(3, "Available");
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Parking slot added successfully.", "Add Slot", JOptionPane.INFORMATION_MESSAGE);
            loadSlots();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to add slot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performEditSlot() {
        // Use the currently selected row in the table; avoid extra picker dialog.
        int row = jTableBookings.getSelectedRow();
        int slotId = -1;
        String slotNumber = null, slotType = null, status = null;
        if (row >= 0 && row < tableModel.getRowCount() && tableModel.getColumnCount() >= 4) {
            Object sidObj = tableModel.getValueAt(row, 0);
            if (sidObj instanceof Number) {
                slotId = ((Number) sidObj).intValue();
                slotNumber = String.valueOf(tableModel.getValueAt(row, 1));
                slotType = String.valueOf(tableModel.getValueAt(row, 2));
                status = String.valueOf(tableModel.getValueAt(row, 3));
            }
        }
        if (slotId < 0) {
            tableModel.setColumnIdentifiers(new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"});
            loadSlots();
            JOptionPane.showMessageDialog(this, "Please select a slot from the table to edit.", "Edit Slot", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTextField numberField = new JTextField(slotNumber, 15);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Car", "Motorcycle"});
        typeCombo.setSelectedItem(slotType);
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Available", "Occupied"});
        statusCombo.setSelectedItem(status);
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Slot number:"));
        panel.add(numberField);
        panel.add(new javax.swing.JLabel("Slot type:"));
        panel.add(typeCombo);
        panel.add(new javax.swing.JLabel("Status:"));
        panel.add(statusCombo);
        if (JOptionPane.showConfirmDialog(this, panel, "Edit Parking Slot", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String newNumber = numberField.getText().trim();
        if (newNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Slot number is required.", "Edit Slot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String newStatus = (String) statusCombo.getSelectedItem();
        String newSlotType = (String) typeCombo.getSelectedItem();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            int activeOnSlot = SlotTransactionUtil.countActiveTransactionsOnSlot(conn, slotId);
            String prevType = slotType == null ? "" : slotType.trim();
            boolean slotTypeChanged = !prevType.equalsIgnoreCase(newSlotType == null ? "" : newSlotType.trim());
            // Reclassifying an empty bay (Car ↔ Motorcycle) should not leave a stale "Occupied" from the old combo.
            if (slotTypeChanged && activeOnSlot == 0) {
                newStatus = "Available";
            }
            if ("Available".equalsIgnoreCase(newStatus) && activeOnSlot > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cannot set this slot to Available: an Active parking session still exists for this bay.\n"
                                + "Complete or release that session in Cashier (Release Slot) first.",
                        "Edit Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_slots SET slot_number = ?, slot_type = ?, status = ? WHERE slot_id = ?")) {
                ps.setString(1, newNumber);
                ps.setString(2, newSlotType);
                ps.setString(3, newStatus);
                ps.setInt(4, slotId);
                ps.executeUpdate();
            }
            // Cashier table shows vehicles.vehicle_type; align active sessions with the slot designation.
            try (PreparedStatement syncV = conn.prepareStatement(
                    "UPDATE vehicles SET vehicle_type = ? WHERE vehicle_id IN ("
                            + "SELECT vehicle_id FROM parking_transactions WHERE slot_id = ? AND lower(trim(status)) = 'active')")) {
                syncV.setString(1, newSlotType);
                syncV.setInt(2, slotId);
                syncV.executeUpdate();
            }
            String okMsg = slotTypeChanged && activeOnSlot == 0
                    ? "Slot type updated. No vehicle checked in on this bay — status set to Available."
                    : "Slot updated successfully.";
            JOptionPane.showMessageDialog(this, okMsg, "Edit Slot", JOptionPane.INFORMATION_MESSAGE);
            loadSlots();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to update slot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performDeleteSlot() {
        // Get selection before refreshing table (loadSlots() clears selection)
        int row = jTableBookings.getSelectedRow();
        int slotId = -1;
        if (row >= 0 && row < tableModel.getRowCount() && tableModel.getColumnCount() >= 4) {
            Object sidObj = tableModel.getValueAt(row, 0);
            if (sidObj instanceof Number) slotId = ((Number) sidObj).intValue();
        }
        if (slotId < 0) {
            tableModel.setColumnIdentifiers(new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"});
            loadSlots();
            JOptionPane.showMessageDialog(this, "Please select a slot from the table to delete.", "Delete Slot", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete this parking slot?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            if (SlotTransactionUtil.countActiveTransactionsOnSlot(conn, slotId) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete this slot: an Active parking session is still linked to it.",
                        "Delete Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM parking_slots WHERE slot_id = ?")) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Slot deleted successfully.", "Delete Slot", JOptionPane.INFORMATION_MESSAGE);
            loadSlots();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete slot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private Object[] showSlotPickerDialog(String title) {
        final List<Object[]> slots = new ArrayList<>();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT slot_id, slot_number, slot_type, status FROM parking_slots ORDER BY slot_id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slots.add(new Object[]{rs.getInt("slot_id"), rs.getString("slot_number"), rs.getString("slot_type"), rs.getString("status")});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load slots: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } finally {
            ConnectionConfig.close(conn);
        }
        if (slots.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No parking slots found. Add slots first.", title, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String[] options = new String[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            Object[] s = slots.get(i);
            options[i] = s[1] + " - " + s[2] + " (" + s[3] + ")";
        }
        JComboBox<String> combo = new JComboBox<>(options);
        combo.setSelectedIndex(0);
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Slot:"));
        panel.add(combo);
        if (JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
        return slots.get(combo.getSelectedIndex());
    }

    private void loadSlots() {
        tableModel.setRowCount(0);
        userIds.clear();
        tableModel.setColumnIdentifiers(new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"});
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT slot_id, slot_number, slot_type, status FROM parking_slots ORDER BY slot_id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{rs.getInt("slot_id"), rs.getString("slot_number"), rs.getString("slot_type"), rs.getString("status")});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load slots: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void loadTransactions() {
        tableModel.setRowCount(0);
        userIds.clear();
        tableModel.setColumnIdentifiers(new String[]{"Transaction ID", "Plate", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"});
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT pt.transaction_id, v.plate_number, v.vehicle_type, pt.slot_id, pt.time_in, pt.time_out, pt.parking_fee, pt.status " +
                    "FROM parking_transactions pt JOIN vehicles v ON pt.vehicle_id = v.vehicle_id ORDER BY pt.transaction_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("transaction_id"),
                            nullToEmpty(rs.getString("plate_number")),
                            nullToEmpty(rs.getString("vehicle_type")),
                            rs.getInt("slot_id"),
                            nullToEmpty(rs.getString("time_in")),
                            nullToEmpty(rs.getString("time_out")),
                            rs.getObject("parking_fee"),
                            nullToEmpty(rs.getString("status"))
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load transactions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performReports() {
        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            double todaySales = 0;
            int todayCount = 0;
            double yesterdaySales = 0;
            int yesterdayCount = 0;
            double totalSales = 0;
            int totalCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) AS total, COUNT(*) AS cnt FROM payments WHERE payment_date LIKE ?")) {
                ps.setString(1, today + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        todaySales = rs.getDouble("total");
                        todayCount = rs.getInt("cnt");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) AS total, COUNT(*) AS cnt FROM payments WHERE payment_date LIKE ?")) {
                ps.setString(1, yesterday + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        yesterdaySales = rs.getDouble("total");
                        yesterdayCount = rs.getInt("cnt");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) AS total, COUNT(*) AS cnt FROM payments")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalSales = rs.getDouble("total");
                        totalCount = rs.getInt("cnt");
                    }
                }
            }
            StyledDialog.showAdminSalesReport(this, "Reports - Sales", today, yesterday,
                    todaySales, todayCount, yesterdaySales, yesterdayCount, totalSales, totalCount);
        } catch (SQLException e) {
            StyledDialog.showPlainMessage(this, "Error", "Failed to load reports:\n" + e.getMessage());
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performDailySales() {
        String today = LocalDate.now().toString();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            double total = 0;
            int count = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) AS total, COUNT(*) AS cnt FROM payments WHERE payment_date LIKE ?")) {
                ps.setString(1, today + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getDouble("total");
                        count = rs.getInt("cnt");
                    }
                }
            }
            StyledDialog.showAdminDailySalesSummary(this, today, total, count);
        } catch (SQLException e) {
            StyledDialog.showPlainMessage(this, "Error", "Failed to load daily sales:\n" + e.getMessage());
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void initComponents() {
        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableBookings = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(790, 415));
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTableBookings.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{},
            new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"}
        ));
        jTableBookings.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(jTableBookings);
        AddSlotPanel = AppTheme.createNavPanel("Add Slot");
        DeleteSlotPanel = AppTheme.createNavPanel("Delete Slot");
        EditSlotPanel = AppTheme.createNavPanel("Edit Slot");
        ViewSlotStatusPanel = AppTheme.createNavPanel("Slot Status");
        ViewTransactionsPanel = AppTheme.createNavPanel("View Transactions");
        ReportsPanel = AppTheme.createNavPanel("REPORTS");
        DailySalesPanel = AppTheme.createNavPanel("DAILY SALES");

        add(mainPanel);
    }
}
