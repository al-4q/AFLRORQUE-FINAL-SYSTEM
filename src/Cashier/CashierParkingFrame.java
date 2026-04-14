package Cashier;

import Admin.InternalPageFrame;
import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.ParkingFeeUtil;
import Configuration.StyledDialog;
import Main.MainPage;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 * Active (non-completed) parking sessions only. Completed sessions appear under
 * Completed Transaction (sidebar). Flow: calculate fee → Record Cash/GCash Payment →
 * Release Slot (blocked until payment is on file). Also: Search, Daily Sales.
 */
public class CashierParkingFrame extends InternalPageFrame {

    private final int currentUserId;
    private final DefaultTableModel tableModel;

    private javax.swing.JPanel SearchVehiclePanel;
    private javax.swing.JPanel CalculateParkingFeePanel;
    private javax.swing.JPanel DailySalesPanel;
    private javax.swing.JPanel RecordGcashPaymentPanel;
    private javax.swing.JPanel ReleaseSlotPanel;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane jScrollPane1;

    public CashierParkingFrame(int currentUserId) {
        super();
        this.currentUserId = currentUserId;
        initComponents();
        tableModel = (DefaultTableModel) jTable1.getModel();
        tableModel.setColumnIdentifiers(new String[]{
                "Transaction ID", "Plate", "User", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"});
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTable1);
        makeContentFillFrame();
        setupPanelListeners();
        loadTransactions();
    }

    private void makeContentFillFrame() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(0, 8));
        javax.swing.JPanel topPanel = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topPanel.setBackground(AppTheme.CONTENT_BG);
        topPanel.add(SearchVehiclePanel);
        topPanel.add(CalculateParkingFeePanel);
        topPanel.add(DailySalesPanel);
        topPanel.add(RecordGcashPaymentPanel);
        topPanel.add(ReleaseSlotPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(jScrollPane1, BorderLayout.CENTER);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void setupPanelListeners() {
        AppTheme.wireNavPanelClick(SearchVehiclePanel, this::performSearchVehicle);
        AppTheme.wireNavPanelClick(CalculateParkingFeePanel, this::performCalculateParkingFee);
        AppTheme.wireNavPanelClick(DailySalesPanel, this::performDailySales);
        AppTheme.wireNavPanelClick(RecordGcashPaymentPanel, this::performRecordPayment);
        AppTheme.wireNavPanelClick(ReleaseSlotPanel, this::performReleaseSlot);

        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
        jTable1.setCursor(handCursor);
        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && jTable1.getSelectedRow() >= 0) {
                    performCalculateParkingFee();
                }
            }
        });
    }

    private void loadTransactions() {
        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(new String[]{
                "Transaction ID", "Plate", "User", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"});
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            // Sync: free bays that have Completed sessions but no Active check-in on that slot.
            // (Do not touch slots that still have an Active transaction — old logic used "ever had Completed"
            // and wrongly cleared Occupied when revisiting Cashier after an admin edit.)
            try (PreparedStatement sync = conn.prepareStatement(
                    "UPDATE parking_slots SET status = 'Available' "
                            + "WHERE slot_id IN (SELECT slot_id FROM parking_transactions WHERE status = 'Completed' AND slot_id IS NOT NULL) "
                            + "AND NOT EXISTS (SELECT 1 FROM parking_transactions pt WHERE pt.slot_id = parking_slots.slot_id AND lower(trim(pt.status)) = 'active')")) {
                sync.executeUpdate();
            }
            String sql = "SELECT pt.transaction_id, v.plate_number, "
                    + "COALESCE((SELECT u.username FROM users u WHERE lower(trim(COALESCE(u.name,''))) = lower(trim(COALESCE(v.owner_name,''))) LIMIT 1), v.owner_name) AS user_label, "
                    + "v.vehicle_type, pt.slot_id, pt.time_in, pt.time_out, pt.parking_fee, pt.status "
                    + "FROM parking_transactions pt JOIN vehicles v ON pt.vehicle_id = v.vehicle_id "
                    + "WHERE lower(trim(pt.status)) <> 'completed' "
                    + "ORDER BY pt.transaction_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("transaction_id"),
                            nullToEmpty(rs.getString("plate_number")),
                            nullToEmpty(rs.getString("user_label")),
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

    private void performSearchVehicle() {
        JTextField searchField = new JTextField(20);
        searchField.setToolTipText("Plate number");
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Search (plate number):"));
        panel.add(searchField);
        if (JOptionPane.showConfirmDialog(this, panel, "Search Parked Vehicle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String q = searchField.getText().trim();
        if (q.isEmpty()) {
            loadTransactions();
            return;
        }
        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(new String[]{
                "Transaction ID", "Plate", "User", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"});
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT pt.transaction_id, v.plate_number, "
                    + "COALESCE((SELECT u.username FROM users u WHERE lower(trim(COALESCE(u.name,''))) = lower(trim(COALESCE(v.owner_name,''))) LIMIT 1), v.owner_name) AS user_label, "
                    + "v.vehicle_type, pt.slot_id, pt.time_in, pt.time_out, pt.parking_fee, pt.status "
                    + "FROM parking_transactions pt JOIN vehicles v ON pt.vehicle_id = v.vehicle_id "
                    + "WHERE v.plate_number LIKE ? AND lower(trim(pt.status)) <> 'completed' "
                    + "ORDER BY pt.transaction_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, "%" + q + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                                rs.getInt("transaction_id"),
                                nullToEmpty(rs.getString("plate_number")),
                                nullToEmpty(rs.getString("user_label")),
                                nullToEmpty(rs.getString("vehicle_type")),
                                rs.getInt("slot_id"),
                                nullToEmpty(rs.getString("time_in")),
                                nullToEmpty(rs.getString("time_out")),
                                rs.getObject("parking_fee"),
                                nullToEmpty(rs.getString("status"))
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to search: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performCalculateParkingFee() {
        int row = jTable1.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Select a transaction row first (e.g. use Search Vehicle).", "Calculate Parking Fee", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object txIdObj = tableModel.getValueAt(row, 0);
        if (!(txIdObj instanceof Number)) return;
        int transactionId = ((Number) txIdObj).intValue();
        Object statusObj = tableModel.getValueAt(row, 8);
        if ("Completed".equalsIgnoreCase(String.valueOf(statusObj))) {
            JOptionPane.showMessageDialog(this, "This transaction is already completed. Cannot modify fee.", "Calculate Parking Fee", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String timeInStr = String.valueOf(tableModel.getValueAt(row, 5));
        String timeOutStr = tableModel.getValueAt(row, 6) != null ? String.valueOf(tableModel.getValueAt(row, 6)) : "";
        boolean stampTimeOut = timeOutStr == null || timeOutStr.trim().isEmpty();
        String timeOutForFee = stampTimeOut
                ? java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : timeOutStr.trim();
        double feePreview = ParkingFeeUtil.calculateFee(timeInStr, timeOutForFee);
        String confirmMsg = stampTimeOut
                ? "Time out will be set to the current time.\nCalculated parking fee: " + feePreview
                + " (rate " + ParkingFeeUtil.HOURLY_RATE + "/hr).\n\nClick OK to save, or Cancel / Close to leave unchanged."
                : "Recalculate parking fee as " + feePreview + " (rate " + ParkingFeeUtil.HOURLY_RATE + "/hr)?\n\n"
                + "Click OK to save, or Cancel / Close to leave unchanged.";
        int decision = JOptionPane.showConfirmDialog(this, confirmMsg, "Calculate Parking Fee",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (decision != JOptionPane.OK_OPTION) {
            return;
        }
        String timeOutToSave = timeOutForFee;
        double feeToSave = feePreview;
        if (stampTimeOut) {
            timeOutToSave = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            feeToSave = ParkingFeeUtil.calculateFee(timeInStr, timeOutToSave);
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            if (stampTimeOut) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_transactions SET time_out = ? WHERE transaction_id = ?")) {
                    ps.setString(1, timeOutToSave);
                    ps.setInt(2, transactionId);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_transactions SET parking_fee = ? WHERE transaction_id = ?")) {
                ps.setDouble(1, feeToSave);
                ps.setInt(2, transactionId);
                ps.executeUpdate();
            }
            loadTransactions();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to update: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performDailySales() {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            double total = 0;
            int count = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) AS total, COUNT(*) AS cnt FROM payments")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getDouble("total");
                        count = rs.getInt("cnt");
                    }
                }
            }
            StyledDialog.showDailySalesSummary(this, total, count);
        } catch (SQLException e) {
            StyledDialog.showPlainMessage(this, "Error", "Failed to load daily sales:\n" + e.getMessage());
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private static boolean paymentMatchesFee(Connection conn, int transactionId, double parkingFee) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT amount FROM payments WHERE transaction_id = ? ORDER BY payment_id DESC LIMIT 1")) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                double paid = rs.getDouble("amount");
                return Math.abs(paid - parkingFee) < 0.02;
            }
        }
    }

    private static boolean hasAnyPayment(Connection conn, int transactionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM payments WHERE transaction_id = ? LIMIT 1")) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Records Cash or GCash payment while the session is still active. Release Slot is blocked until payment exists.
     */
    private void performRecordPayment() {
        final String dlgTitle = "Record Cash/GCash Payment";
        int row = jTable1.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Select a transaction row first. Use Search Vehicle to load data.", dlgTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object txIdObj = tableModel.getValueAt(row, 0);
        if (!(txIdObj instanceof Number)) {
            return;
        }
        int transactionId = ((Number) txIdObj).intValue();
        Object statusObj = tableModel.getValueAt(row, 8);
        if ("Completed".equalsIgnoreCase(String.valueOf(statusObj))) {
            JOptionPane.showMessageDialog(this, "This session is already completed.", dlgTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object feeObj = tableModel.getValueAt(row, 7);
        if (feeObj == null || !(feeObj instanceof Number) || ((Number) feeObj).doubleValue() <= 0) {
            JOptionPane.showMessageDialog(this, "Calculate the parking fee first (Calculate Parking Fee), then record payment after the customer pays.", dlgTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        double fee = ((Number) feeObj).doubleValue();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            if (hasAnyPayment(conn, transactionId)) {
                JOptionPane.showMessageDialog(this, "A payment is already recorded for this transaction.", dlgTitle, JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to check payments: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            ConnectionConfig.close(conn);
        }

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.LINE_START;
        int y = 0;
        gc.gridx = 0;
        gc.gridy = y;
        form.add(new JLabel("Parking fee:"), gc);
        gc.gridx = 1;
        form.add(new JLabel(String.format(Locale.US, "₱%.2f", fee)), gc);
        y++;
        gc.gridx = 0;
        gc.gridy = y;
        form.add(new JLabel("Payment type:"), gc);
        gc.gridx = 1;
        JComboBox<String> payType = new JComboBox<>(new String[]{"Cash", "GCash"});
        form.add(payType, gc);
        y++;
        gc.gridx = 0;
        gc.gridy = y;
        form.add(new JLabel("Cash received (₱):"), gc);
        gc.gridx = 1;
        JTextField tfTender = new JTextField(14);
        form.add(tfTender, gc);
        y++;
        gc.gridx = 0;
        gc.gridy = y;
        form.add(new JLabel("Change (auto):"), gc);
        gc.gridx = 1;
        JLabel changeDisplay = new JLabel("—");
        changeDisplay.setFont(changeDisplay.getFont().deriveFont(Font.BOLD));
        form.add(changeDisplay, gc);

        Runnable updateChangePreview = () -> {
            if (!"Cash".equals(payType.getSelectedItem())) {
                changeDisplay.setText("—");
                return;
            }
            String ts = tfTender.getText().trim();
            if (ts.isEmpty()) {
                changeDisplay.setText("—");
                return;
            }
            try {
                double tendered = Double.parseDouble(ts.replace(",", ""));
                if (tendered + 1e-9 < fee) {
                    changeDisplay.setText(String.format(Locale.US, "(min ₱%.2f)", fee));
                } else {
                    changeDisplay.setText(String.format(Locale.US, "₱%.2f", tendered - fee));
                }
            } catch (NumberFormatException ex) {
                changeDisplay.setText("—");
            }
        };
        tfTender.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateChangePreview.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateChangePreview.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateChangePreview.run();
            }
        });

        Runnable syncCashFields = () -> {
            boolean cash = "Cash".equals(payType.getSelectedItem());
            tfTender.setEnabled(cash);
            changeDisplay.setEnabled(cash);
            if (!cash) {
                tfTender.setText("");
                changeDisplay.setText("—");
            } else {
                updateChangePreview.run();
            }
        };
        payType.addActionListener(e -> syncCashFields.run());
        syncCashFields.run();

        if (JOptionPane.showConfirmDialog(this, form, dlgTitle, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }

        boolean isCash = "Cash".equals(payType.getSelectedItem());
        Double cashTendered = null;
        Double changeAmt = null;
        String method = isCash ? "Cash" : "GCash";

        if (isCash) {
            String ts = tfTender.getText().trim();
            if (ts.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter the cash amount received from the customer.", dlgTitle, JOptionPane.WARNING_MESSAGE);
                return;
            }
            double tendered;
            try {
                tendered = Double.parseDouble(ts.replace(",", ""));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid number for cash received.", dlgTitle, JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (tendered < 0) {
                JOptionPane.showMessageDialog(this, "Cash received cannot be negative.", dlgTitle, JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (tendered + 1e-9 < fee) {
                JOptionPane.showMessageDialog(this,
                        String.format(Locale.US, "Cash received must be at least the parking fee (₱%.2f).", fee),
                        dlgTitle, JOptionPane.WARNING_MESSAGE);
                return;
            }
            cashTendered = tendered;
            changeAmt = tendered - fee;
        } else {
            if (JOptionPane.showConfirmDialog(this,
                    String.format(Locale.US,
                            "Confirm GCash payment received:%n%nAmount: ₱%.2f%n%nUse only after the customer has sent payment via GCash.",
                            fee),
                    dlgTitle, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
        }

        conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String paymentDate = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            try (PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO payments (transaction_id, cashier_id, amount, payment_date, payment_method, cash_tendered, change_amount) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ins.setInt(1, transactionId);
                ins.setInt(2, currentUserId);
                ins.setDouble(3, fee);
                ins.setString(4, paymentDate);
                ins.setString(5, method);
                if (cashTendered != null) {
                    ins.setDouble(6, cashTendered);
                } else {
                    ins.setNull(6, Types.REAL);
                }
                if (changeAmt != null) {
                    ins.setDouble(7, changeAmt);
                } else {
                    ins.setNull(7, Types.REAL);
                }
                ins.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Payment recorded. You can release the slot when ready.", dlgTitle, JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to record payment: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performReleaseSlot() {
        int row = jTable1.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Select a transaction row first. Use Search Vehicle to load data.", "Release Parking Slot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object txIdObj = tableModel.getValueAt(row, 0);
        Object slotIdObj = tableModel.getValueAt(row, 4);
        Object statusObj = tableModel.getValueAt(row, 8);
        if (!(txIdObj instanceof Number)) return;
        if ("Completed".equalsIgnoreCase(String.valueOf(statusObj))) {
            // Sync: ensure slot is Available even if it wasn't updated before
            int slotId = slotIdObj instanceof Number ? ((Number) slotIdObj).intValue() : 0;
            if (slotId > 0) {
                Connection connSync = null;
                try {
                    connSync = ConnectionConfig.getConnection();
                    try (PreparedStatement up = connSync.prepareStatement("UPDATE parking_slots SET status = 'Available' WHERE slot_id = ?")) {
                        up.setInt(1, slotId);
                        up.executeUpdate();
                    }
                    loadTransactions();
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "Failed to sync slot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    ConnectionConfig.close(connSync);
                }
            }
            JOptionPane.showMessageDialog(this, "This transaction is already completed. Slot updated to Available.", "Release Parking Slot", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int transactionId = ((Number) txIdObj).intValue();
        Object feeObj = tableModel.getValueAt(row, 7);
        if (feeObj == null || !(feeObj instanceof Number) || ((Number) feeObj).doubleValue() <= 0) {
            JOptionPane.showMessageDialog(this, "Please calculate the parking fee first (click Calculate Parking Fee), then you can release the slot.", "Release Parking Slot", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double parkingFee = ((Number) feeObj).doubleValue();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            if (!paymentMatchesFee(conn, transactionId, parkingFee)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot release the slot yet. The customer must pay first.\n\n"
                                + "After payment is received, use \"Record Cash/GCash Payment\", then release the slot.\n"
                                + "If the fee was changed after payment, record payment again or align the fee with the paid amount.",
                        "Release Parking Slot", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to verify payment: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            ConnectionConfig.close(conn);
        }
        if (JOptionPane.showConfirmDialog(this, "Payment is on file. Release the parking slot and complete this session?", "Release Parking Slot", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            int slotId = slotIdObj instanceof Number ? ((Number) slotIdObj).intValue() : 0;
            if (slotId > 0) {
                try (PreparedStatement up = conn.prepareStatement("UPDATE parking_slots SET status = 'Available' WHERE slot_id = ?")) {
                    up.setInt(1, slotId);
                    up.executeUpdate();
                }
            }
            String timeOut = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_transactions SET time_out = ?, status = 'Completed' WHERE transaction_id = ?")) {
                ps.setString(1, timeOut);
                ps.setInt(2, transactionId);
                ps.executeUpdate();
            }

            // Also mark any related reservation (previously 'Used') as 'Completed' for this slot
            if (slotId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE reservations SET status = 'Completed' WHERE slot_id = ? AND status = 'Used'")) {
                    ps.setInt(1, slotId);
                    ps.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(this, "Slot released. Session completed.", "Release Parking Slot", JOptionPane.INFORMATION_MESSAGE);
            loadTransactions();
            SwingUtilities.invokeLater(() -> {
                MainPage host = MainPage.findHostingMainPage(CashierParkingFrame.this);
                if (host != null) {
                    host.openCompletedTransactions();
                }
            });
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to release slot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void initComponents() {
        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(790, 415));
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][]{},
            new String[]{"Transaction ID", "Plate", "User", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"}
        ));
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(jTable1);
        SearchVehiclePanel = AppTheme.createNavPanel("Search Vehicle");
        CalculateParkingFeePanel = AppTheme.createNavPanel("Calculate Parking Fee");
        DailySalesPanel = AppTheme.createNavPanel("Daily Sales");
        RecordGcashPaymentPanel = AppTheme.createNavPanel("Record Cash/GCash Payment");
        ReleaseSlotPanel = AppTheme.createNavPanel("Release Slot");

        add(mainPanel);
    }
}
