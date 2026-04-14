package User;

import Cashier.ReceiptDialogHelper;
import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.PasswordUtil;
import Main.Mainframe;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.ExecutionException;
import javax.swing.table.TableColumn;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class UserDash extends InternalPageFrame {

    private final DefaultTableModel tableModel;
    private final List<Integer> userIds = new ArrayList<>();
    private static final String[] USER_COLUMNS = {"User ID", "Role", "Username", "Name"};
    private final int currentUserId;
    private String currentUserName = "";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private javax.swing.Timer timeLeftTimer;

    private javax.swing.ImageIcon safeIcon(String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url == null) return null;
            return new javax.swing.ImageIcon(url);
        } catch (Exception ignored) {
            return null;
        }
    }

    public UserDash(int currentUserId) {
        this.currentUserId = currentUserId;
        initComponents();
        tableModel = (DefaultTableModel) jTable1.getModel();
        tableModel.setColumnIdentifiers(USER_COLUMNS);
        tableModel.setRowCount(0);
        applyModernTheme();
        setupPanelListeners();
        loadCurrentUser();
        startTimeLeftTimer();
    }

    private void applyModernTheme() {
        // Deep navy + teal, matching login/register, keep layout intact
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTable1);

        Color navBg   = AppTheme.NAV_BTN_BG;
        Color navText = AppTheme.TEXT_ON_NAV;
        java.awt.Font navFont = AppTheme.FONT_NAV;
        Color cardBg  = AppTheme.CARD_BG;
        Color text    = AppTheme.TEXT_PRIMARY;
        Color muted   = AppTheme.TEXT_MUTED;

        EditPasswordPanel.setBackground(navBg);
        BookingHistoryPanel.setBackground(navBg);
        ViewReceiptsPanel.setBackground(navBg);

        EditUserText.setForeground(navText);
        EditUserText.setFont(navFont);
        EditUserText2.setForeground(navText);
        EditUserText2.setFont(navFont);
        EditUserText1.setForeground(navText);
        EditUserText1.setFont(navFont);

        UserPanel.setBackground(cardBg);
        UserPanel2.setBackground(cardBg);
        UserName.setForeground(text);
        Name.setForeground(text);
        UserID.setForeground(text);
        Email.setForeground(text);
        TimeLeft.setForeground(muted);
    }

    private void applyThemeToPanels() {
        EditPasswordPanel.setBackground(AppTheme.NAV_BTN_BG);
        EditUserText.setForeground(AppTheme.TEXT_ON_NAV);
        EditUserText.setFont(AppTheme.FONT_NAV);
        BookingHistoryPanel.setBackground(AppTheme.NAV_BTN_BG);
        EditUserText2.setForeground(AppTheme.TEXT_ON_NAV);
        EditUserText2.setFont(AppTheme.FONT_NAV);
        ViewReceiptsPanel.setBackground(AppTheme.NAV_BTN_BG);
        EditUserText1.setForeground(AppTheme.TEXT_ON_NAV);
        EditUserText1.setFont(AppTheme.FONT_NAV);
        UserPanel.setBackground(AppTheme.NAV_BTN_BG);
        UserPanel2.setBackground(AppTheme.CARD_BG);
    }

    private void loadCurrentUser() {
        loadUsersFromDb(currentUserId);
    }

    private void loadUsersFromDb(Integer filterById) {
        tableModel.setRowCount(0);
        userIds.clear();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            int id = filterById != null ? filterById : currentUserId;
            String sql = "SELECT user_id, role, username, name FROM users WHERE user_id = ? ORDER BY user_id";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        userIds.add(rs.getInt("user_id"));
                        tableModel.addRow(new Object[]{
                            rs.getInt("user_id"),
                            rs.getString("role"),
                            rs.getString("username"),
                            rs.getString("name")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
        updateUserInfoPanel();
    }

    private void updateUserInfoPanel() {
        if (tableModel == null || tableModel.getRowCount() == 0) {
            return;
        }
        int row = 0;
        Object id = tableModel.getValueAt(row, 0);
        Object username = tableModel.getValueAt(row, 2);
        Object name = tableModel.getValueAt(row, 3);
        currentUserName = name != null ? name.toString() : "";
        UserID.setText("User ID: " + String.valueOf(id));
        UserName.setText("Username: " + String.valueOf(username));
        Name.setText("Name: " + String.valueOf(name));
        Email.setText("");
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        EditPasswordPanel.setCursor(handCursor);
        EditPasswordPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performChangePassword(); }
        });
        BookingHistoryPanel.setCursor(handCursor);
        BookingHistoryPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { openMyBookings(); }
        });
        ViewReceiptsPanel.setCursor(handCursor);
        ViewReceiptsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performViewParkingFee(); }
        });

        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && jTable1.getSelectedRow() >= 0) {
                    performEditProfile();
                }
            }
        });
    }

    /**
     * Result of loading the latest printed receipt in one query (background thread).
     */
    private static final class PrintedReceiptData {
        final int transactionId;
        final String plate;
        final String vehicleType;
        final int slotId;
        final String timeIn;
        final String timeOut;
        final Object parkingFeeObj;
        final String status;
        final double amount;
        final String paymentMethod;
        final String customerDisplay;
        final Double cashTendered;
        final Double changeAmount;

        PrintedReceiptData(int transactionId, String plate, String vehicleType, int slotId, String timeIn, String timeOut,
                Object parkingFeeObj, String status, double amount, String paymentMethod, String customerDisplay,
                Double cashTendered, Double changeAmount) {
            this.transactionId = transactionId;
            this.plate = plate;
            this.vehicleType = vehicleType;
            this.slotId = slotId;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
            this.parkingFeeObj = parkingFeeObj;
            this.status = status;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.customerDisplay = customerDisplay;
            this.cashTendered = cashTendered;
            this.changeAmount = changeAmount;
        }
    }

    private void performViewParkingFee() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<PrintedReceiptData, Void> worker = new SwingWorker<PrintedReceiptData, Void>() {
            @Override
            protected PrintedReceiptData doInBackground() throws Exception {
                try (Connection conn = ConnectionConfig.getConnection()) {
                    Set<String> paymentColumns = getTableColumns(conn, "payments");
                    String cashTenderedExpr = paymentColumns.contains("cash_tendered")
                            ? "p.cash_tendered"
                            : "NULL";
                    String changeAmountExpr = paymentColumns.contains("change_amount")
                            ? "p.change_amount"
                            : "NULL";
                    String sql = "SELECT pt.transaction_id, v.plate_number, v.vehicle_type, pt.slot_id, pt.time_in, pt.time_out, "
                            + "pt.parking_fee, pt.status, p.amount, "
                            + "COALESCE(NULLIF(trim(p.payment_method), ''), 'Cash') AS payment_method, "
                            + cashTenderedExpr + " AS cash_tendered, "
                            + changeAmountExpr + " AS change_amount, "
                            + "COALESCE((SELECT u2.username FROM users u2 "
                            + "WHERE lower(trim(COALESCE(u2.name,''))) = lower(trim(COALESCE(v.owner_name,''))) LIMIT 1), "
                            + "NULLIF(trim(v.owner_name), ''), '\u2014') AS customer_display "
                            + "FROM parking_transactions pt "
                            + "INNER JOIN vehicles v ON pt.vehicle_id = v.vehicle_id "
                            + "INNER JOIN users u ON u.user_id = ? "
                            + "INNER JOIN payments p ON p.payment_id = ("
                            + "SELECT payment_id FROM payments WHERE transaction_id = pt.transaction_id "
                            + "ORDER BY payment_id DESC LIMIT 1) "
                            + "WHERE pt.status = 'Completed' "
                            + "AND (lower(trim(COALESCE(v.owner_name,''))) = lower(trim(COALESCE(u.name,''))) "
                            + "     OR lower(trim(COALESCE(v.owner_name,''))) = lower(trim(COALESCE(u.username,'')))) "
                            + "AND COALESCE(p.receipt_printed, 0) = 1 "
                            + "ORDER BY pt.transaction_id DESC LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, currentUserId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return null;
                        }
                        int tid = rs.getInt("transaction_id");
                        String pm = rs.getString("payment_method");
                        if (pm == null || pm.trim().isEmpty()) {
                            pm = "Cash";
                        }
                        Object feeObj = rs.getObject("parking_fee");
                        Double ct = null;
                        Double ch = null;
                        Object oCt = rs.getObject("cash_tendered");
                        if (oCt != null) {
                            ct = rs.getDouble("cash_tendered");
                        }
                        Object oCh = rs.getObject("change_amount");
                        if (oCh != null) {
                            ch = rs.getDouble("change_amount");
                        }
                        return new PrintedReceiptData(
                                tid,
                                nullToEmpty(rs.getString("plate_number")),
                                nullToEmpty(rs.getString("vehicle_type")),
                                rs.getInt("slot_id"),
                                nullToEmpty(rs.getString("time_in")),
                                nullToEmpty(rs.getString("time_out")),
                                feeObj,
                                nullToEmpty(rs.getString("status")),
                                rs.getDouble("amount"),
                                pm,
                                nullToEmpty(rs.getString("customer_display")),
                                ct,
                                ch);
                    }
                }
                }
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    PrintedReceiptData data = get();
                    if (data == null) {
                        JOptionPane.showMessageDialog(UserDash.this,
                                "No receipt is available yet. After checkout, the cashier must open Generate Receipt "
                                        + "(Completed Transactions) and print your receipt. You can view it here only after it has been printed.",
                                "Your parking receipt",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    Object feeCell = data.parkingFeeObj != null ? data.parkingFeeObj : "N/A";
                    DefaultTableModel receiptRow = new DefaultTableModel(
                            new Object[][]{{data.transactionId, data.plate, data.vehicleType, data.slotId, data.timeIn, data.timeOut,
                                    feeCell, data.status}},
                            new String[]{"Transaction ID", "Plate", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"});
                    ReceiptDialogHelper.showCustomerReceiptPreview(UserDash.this, receiptRow, 0, data.amount,
                            data.customerDisplay, data.transactionId, data.paymentMethod, false, data.cashTendered, data.changeAmount);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    String msg = c != null ? c.getMessage() : ex.getMessage();
                    JOptionPane.showMessageDialog(UserDash.this,
                            "Failed to load receipt: " + (msg != null ? msg : ex.toString()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private static Set<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    columns.add(name.toLowerCase());
                }
            }
        }
        return columns;
    }

    private void openMyBookings() {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            // Same as classic layout but Time In before Time Out (not Time Out before Time In).
            DefaultTableModel tm = new DefaultTableModel(
                    new String[]{"Transaction ID", "Vehicle", "Slot ID", "Time In", "Time Out", "Fee", "Status"}, 0);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT pt.transaction_id, v.plate_number, pt.slot_id, pt.time_in, pt.time_out, pt.parking_fee, pt.status " +
                    "FROM parking_transactions pt JOIN vehicles v ON pt.vehicle_id = v.vehicle_id " +
                    "WHERE v.owner_name = ? ORDER BY pt.transaction_id DESC")) {
                ps.setString(1, currentUserName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tm.addRow(new Object[]{
                                rs.getInt("transaction_id"),
                                nullToEmpty(rs.getString("plate_number")),
                                rs.getInt("slot_id"),
                                nullToEmpty(rs.getString("time_in")),
                                nullToEmpty(rs.getString("time_out")),
                                rs.getObject("parking_fee"),
                                nullToEmpty(rs.getString("status"))
                        });
                    }
                }
            }
            JTable table = new JTable(tm);
            table.setEnabled(false);
            styleParkingHistoryTable(table);
            JScrollPane scroll = new JScrollPane(table);
            layoutParkingHistoryColumns(table, scroll);
            JOptionPane.showMessageDialog(this, scroll, "Parking History", JOptionPane.PLAIN_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load parking history: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    /** Light header (not sidebar navy); row height and grid like other themed tables. */
    private static void styleParkingHistoryTable(JTable table) {
        table.setRowHeight(Math.max(table.getRowHeight(), 24));
        table.setFont(AppTheme.FONT_LABEL);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setGridColor(AppTheme.BORDER);
        table.setShowGrid(true);
        table.getTableHeader().setFont(AppTheme.FONT_NAV);
        table.getTableHeader().setBackground(AppTheme.CONTENT_BG);
        table.getTableHeader().setForeground(AppTheme.TEXT_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static void layoutParkingHistoryColumns(JTable table, JScrollPane scroll) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        javax.swing.table.TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(110); // Transaction ID
        cm.getColumn(1).setPreferredWidth(100); // Vehicle
        cm.getColumn(2).setPreferredWidth(60); // Slot ID
        cm.getColumn(3).setPreferredWidth(210); // Time In
        cm.getColumn(4).setPreferredWidth(210); // Time Out
        cm.getColumn(5).setPreferredWidth(72); // Fee
        cm.getColumn(6).setPreferredWidth(100); // Status
        int tableW = table.getColumnModel().getTotalColumnWidth() + 6;
        scroll.setPreferredSize(new Dimension(Math.min(980, Math.max(760, tableW + 24)), 320));
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int vw = scroll.getViewport().getWidth();
                if (vw <= 2) {
                    return;
                }
                int total = table.getColumnModel().getTotalColumnWidth();
                int extra = vw - total;
                if (extra > 2) {
                    TableColumn last = cm.getColumn(cm.getColumnCount() - 1);
                    int base = Math.max(last.getWidth(), last.getPreferredWidth());
                    last.setPreferredWidth(base + extra);
                }
            }
        });
        SwingUtilities.invokeLater(() -> {
            int vw = scroll.getViewport().getWidth();
            if (vw <= 2) {
                return;
            }
            int total = table.getColumnModel().getTotalColumnWidth();
            int extra = vw - total;
            if (extra > 2) {
                TableColumn last = cm.getColumn(cm.getColumnCount() - 1);
                int base = Math.max(last.getWidth(), last.getPreferredWidth());
                last.setPreferredWidth(base + extra);
            }
        });
    }

    private void startTimeLeftTimer() {
        if (timeLeftTimer != null) {
            timeLeftTimer.stop();
        }
        updateTimeLeftLabel();
        timeLeftTimer = new javax.swing.Timer(60_000, e -> updateTimeLeftLabel());
        timeLeftTimer.start();
    }

    private void updateTimeLeftLabel() {
        if (TimeLeft == null) return;
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT pt.status, pt.time_in FROM parking_transactions pt " +
                    "JOIN vehicles v ON pt.vehicle_id = v.vehicle_id WHERE v.owner_name = ? ORDER BY pt.transaction_id DESC LIMIT 1")) {
                ps.setString(1, currentUserName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String status = nullToEmpty(rs.getString("status"));
                        String timeIn = nullToEmpty(rs.getString("time_in"));
                        TimeLeft.setText("Status: " + status + (timeIn.isEmpty() ? "" : " | Time in: " + timeIn));
                    } else {
                        TimeLeft.setText("Status: N/A");
                    }
                }
            }
        } catch (SQLException e) {
            TimeLeft.setText("Status: error");
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void performEditProfile() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Profile not loaded.", "Settings", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int row = 0;
        JTextField usernameField = new JTextField(String.valueOf(tableModel.getValueAt(row, 2)), 20);
        JTextField nameField = new JTextField(String.valueOf(tableModel.getValueAt(row, 3)), 20);
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new javax.swing.JLabel("Name:"));
        panel.add(nameField);
        if (JOptionPane.showConfirmDialog(this, panel, "Edit Profile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        if (username.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and name are required.", "Edit Profile", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement check = conn.prepareStatement("SELECT user_id FROM users WHERE username = ? AND user_id != ?")) {
                check.setString(1, username);
                check.setInt(2, currentUserId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "Username already in use. Choose another.", "Edit Profile", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET username = ?, name = ? WHERE user_id = ?")) {
                ps.setString(1, username);
                ps.setString(2, name);
                ps.setInt(3, currentUserId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Profile updated successfully.", "Edit Profile", JOptionPane.INFORMATION_MESSAGE);
            loadCurrentUser();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to update profile: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performChangePassword() {
        JTextField currentPasswordField = new JTextField(20);
        JTextField newPasswordField = new JTextField(20);
        JTextField confirmField = new JTextField(20);
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Current password:"));
        panel.add(currentPasswordField);
        panel.add(new javax.swing.JLabel("New password:"));
        panel.add(newPasswordField);
        panel.add(new javax.swing.JLabel("Confirm new password:"));
        panel.add(confirmField);
        if (JOptionPane.showConfirmDialog(this, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText().trim();
        String confirm = confirmField.getText();
        if (newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "New password cannot be empty.", "Change Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "New password and confirmation do not match.", "Change Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT password FROM users WHERE user_id = ?")) {
                ps.setInt(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !PasswordUtil.verifyPassword(currentPassword, rs.getString("password"))) {
                        JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Change Password", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?")) {
                ps.setString(1, PasswordUtil.hashPassword(newPassword));
                ps.setInt(2, currentUserId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Password changed successfully.", "Change Password", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to change password: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performAddUser() {
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "cashier", "user"});
        JTextField usernameField = new JTextField(20);
        JTextField nameField = new JTextField(20);
        JTextField passwordField = new JTextField(20);
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Role:"));
        panel.add(roleCombo);
        panel.add(new javax.swing.JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new javax.swing.JLabel("Name:"));
        panel.add(nameField);
        panel.add(new javax.swing.JLabel("Password:"));
        panel.add(passwordField);
        if (JOptionPane.showConfirmDialog(this, panel, "Add User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String role = (String) roleCombo.getSelectedItem();
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String password = passwordField.getText().trim();
        if (role.isEmpty() || username.isEmpty() || name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Add User", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement check = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "Username already exists.", "Add User", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO users (name, username, password, role) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, name);
                ps.setString(2, username);
                ps.setString(3, PasswordUtil.hashPassword(password));
                ps.setString(4, role);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "User added successfully.", "Add User", JOptionPane.INFORMATION_MESSAGE);
            loadUsersFromDb(null);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to add user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performEditUser() {
        int row = jTable1.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a user to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "cashier", "user"});
        roleCombo.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 1)));
        JTextField usernameField = new JTextField(String.valueOf(tableModel.getValueAt(row, 2)), 20);
        JTextField nameField = new JTextField(String.valueOf(tableModel.getValueAt(row, 3)), 20);
        JTextField passwordField = new JTextField("", 20);
        passwordField.setToolTipText("Leave blank to keep current password");
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Role:"));
        panel.add(roleCombo);
        panel.add(new javax.swing.JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new javax.swing.JLabel("Name:"));
        panel.add(nameField);
        panel.add(new javax.swing.JLabel("New password (optional):"));
        panel.add(passwordField);
        if (JOptionPane.showConfirmDialog(this, panel, "Edit User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String userType = (String) roleCombo.getSelectedItem();
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String newPassword = passwordField.getText().trim();
        if (userType.isEmpty() || username.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Role, username and name are required.", "Edit User", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement check = conn.prepareStatement("SELECT user_id FROM users WHERE username = ? AND user_id != ?")) {
                check.setString(1, username);
                check.setInt(2, id);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "Username already exists. Choose another.", "Edit User", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            if (!newPassword.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET role = ?, username = ?, name = ?, password = ? WHERE user_id = ?")) {
                    ps.setString(1, userType);
                    ps.setString(2, username);
                    ps.setString(3, name);
                    ps.setString(4, PasswordUtil.hashPassword(newPassword));
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET role = ?, username = ?, name = ? WHERE user_id = ?")) {
                    ps.setString(1, userType);
                    ps.setString(2, username);
                    ps.setString(3, name);
                    ps.setInt(4, id);
                    ps.executeUpdate();
                }
            }
            loadUsersFromDb(null);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to update user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performDeleteUser() {
        int row = jTable1.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete this user?", "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            loadUsersFromDb(null);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to delete user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        EditPasswordPanel = new javax.swing.JPanel();
        EditUserText = new javax.swing.JLabel();
        BookingHistoryPanel = new javax.swing.JPanel();
        EditUserText2 = new javax.swing.JLabel();
        UserPanel = new javax.swing.JPanel();
        UserName = new javax.swing.JLabel();
        Name = new javax.swing.JLabel();
        UserID = new javax.swing.JLabel();
        Email = new javax.swing.JLabel();
        TimeLeft = new javax.swing.JLabel();
        UserPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        ViewReceiptsPanel = new javax.swing.JPanel();
        EditUserText1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(790, 415));

        mainPanel.setBackground(new java.awt.Color(153, 153, 153));
        mainPanel.setPreferredSize(new java.awt.Dimension(570, 450));
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        EditPasswordPanel.setBackground(new java.awt.Color(45, 55, 72));

        EditUserText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        EditUserText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EditUserText.setText("EDIT PASSWORD");

        javax.swing.GroupLayout EditPasswordPanelLayout = new javax.swing.GroupLayout(EditPasswordPanel);
        EditPasswordPanel.setLayout(EditPasswordPanelLayout);
        EditPasswordPanelLayout.setHorizontalGroup(
            EditPasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, EditPasswordPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(EditUserText, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        EditPasswordPanelLayout.setVerticalGroup(
            EditPasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, EditPasswordPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(EditUserText, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanel.add(EditPasswordPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 10, 170, 40));

        BookingHistoryPanel.setBackground(new java.awt.Color(45, 55, 72));

        EditUserText2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        EditUserText2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EditUserText2.setText("PARKING HISTORY");

        javax.swing.GroupLayout BookingHistoryPanelLayout = new javax.swing.GroupLayout(BookingHistoryPanel);
        BookingHistoryPanel.setLayout(BookingHistoryPanelLayout);
        BookingHistoryPanelLayout.setHorizontalGroup(
            BookingHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BookingHistoryPanelLayout.createSequentialGroup()
                .addComponent(EditUserText2, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        BookingHistoryPanelLayout.setVerticalGroup(
            BookingHistoryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BookingHistoryPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(EditUserText2, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanel.add(BookingHistoryPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 60, 170, 40));

        UserPanel.setBackground(new java.awt.Color(30, 41, 59));

        UserName.setFont(new java.awt.Font("Bahnschrift", 1, 12)); // NOI18N
        UserName.setText("Username:");

        Name.setFont(new java.awt.Font("Bahnschrift", 1, 12)); // NOI18N
        Name.setText("Name:");

        UserID.setFont(new java.awt.Font("Bahnschrift", 1, 12)); // NOI18N
        UserID.setText("User ID:");

        Email.setFont(new java.awt.Font("Bahnschrift", 1, 12)); // NOI18N
        Email.setText("Email:");

        TimeLeft.setFont(new java.awt.Font("Bahnschrift", 1, 11)); // NOI18N
        TimeLeft.setText("Status: N/A");

        UserPanel2.setBackground(new java.awt.Color(15, 23, 42));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/icons8-profile-100.png"))); // NOI18N

        javax.swing.GroupLayout UserPanel2Layout = new javax.swing.GroupLayout(UserPanel2);
        UserPanel2.setLayout(UserPanel2Layout);
        UserPanel2Layout.setHorizontalGroup(
            UserPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
        );
        UserPanel2Layout.setVerticalGroup(
            UserPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout UserPanelLayout = new javax.swing.GroupLayout(UserPanel);
        UserPanel.setLayout(UserPanelLayout);
        UserPanelLayout.setHorizontalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(UserPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Name)
                    .addComponent(UserName)
                    .addComponent(UserID)
                    .addComponent(Email)
                    .addComponent(TimeLeft))
                .addGap(0, 181, Short.MAX_VALUE))
        );
        UserPanelLayout.setVerticalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, UserPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(UserName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Name)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(UserID)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Email)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(TimeLeft)
                .addGap(32, 32, 32))
            .addGroup(UserPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(UserPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mainPanel.add(UserPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 10, 380, 140));

        ViewReceiptsPanel.setBackground(new java.awt.Color(45, 55, 72));

        EditUserText1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        EditUserText1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EditUserText1.setText("PARKING RECEIPTS");

        javax.swing.GroupLayout ViewReceiptsPanelLayout = new javax.swing.GroupLayout(ViewReceiptsPanel);
        ViewReceiptsPanel.setLayout(ViewReceiptsPanelLayout);
        ViewReceiptsPanelLayout.setHorizontalGroup(
            ViewReceiptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ViewReceiptsPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(EditUserText1, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        ViewReceiptsPanelLayout.setVerticalGroup(
            ViewReceiptsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ViewReceiptsPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(EditUserText1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanel.add(ViewReceiptsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 110, 170, 40));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        mainPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 160, 620, 280));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 651, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel BookingHistoryPanel;
    private javax.swing.JPanel EditPasswordPanel;
    private javax.swing.JLabel EditUserText;
    private javax.swing.JLabel EditUserText1;
    private javax.swing.JLabel EditUserText2;
    private javax.swing.JLabel Email;
    private javax.swing.JLabel Name;
    private javax.swing.JLabel TimeLeft;
    private javax.swing.JLabel UserID;
    private javax.swing.JLabel UserName;
    private javax.swing.JPanel UserPanel;
    private javax.swing.JPanel UserPanel2;
    private javax.swing.JPanel ViewReceiptsPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables
}
