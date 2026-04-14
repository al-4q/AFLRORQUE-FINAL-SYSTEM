package Admin;

import Configuration.AppTheme;
import Configuration.StyledDialog;
import Configuration.ConnectionConfig;
import Configuration.PasswordUtil;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class AdminSettings extends InternalPageFrame {

    private final DefaultTableModel tableModel;
    private final List<Integer> userIds = new ArrayList<>();
    private static final String[] USER_COLUMNS = {"User ID", "Role", "Username", "Name", "Status"};
    private final int currentUserId;

    private enum ViewMode {
        USERS,
        PARKING_STATS
    }

    private ViewMode currentMode = ViewMode.USERS;

    public AdminSettings(int currentUserId) {
        this.currentUserId = currentUserId;
        initComponents();
        tableModel = (DefaultTableModel) jTableBookings.getModel();
        tableModel.setColumnIdentifiers(USER_COLUMNS);
        applyModernTheme();
        setupPanelListeners();
        loadUsersFromDb(null);
        loadLoggedInUserDetails();
    }

    private void applyModernTheme() {
        // Use shared AppTheme colors so admin matches login/register
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTableBookings);

        java.awt.Color navBg   = AppTheme.NAV_BTN_BG;
        java.awt.Color navText = AppTheme.TEXT_ON_NAV;
        java.awt.Font  navFont = AppTheme.FONT_NAV;
        java.awt.Color cardBg  = AppTheme.CARD_BG;

        // Top action buttons
        EditUserPanel.setBackground(navBg);
        AddUserPanel.setBackground(navBg);
        RemoveUserPanel.setBackground(navBg);
        ApproveUserPanel.setBackground(navBg);

        EditUserText.setForeground(navText);
        EditUserText.setFont(navFont);
        AddUserText.setForeground(navText);
        AddUserText.setFont(navFont);
        RemoveUserText.setForeground(navText);
        RemoveUserText.setFont(navFont);
        ApproveUserText.setForeground(navText);
        ApproveUserText.setFont(navFont);

        // User info card (dark panel: profile text must stay light)
        UserOutPanel.setBackground(navBg);
        UserPanel.setBackground(cardBg);
        UserName.setForeground(navText);
        Name.setForeground(navText);
        UserID.setForeground(navText);
        Email.setForeground(navText);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void loadUsersFromDb(Integer filterById) {
        tableModel.setRowCount(0);
        userIds.clear();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT user_id, role, username, name, COALESCE(status, 'approved') AS status FROM users";
            if (filterById != null) {
                sql += " WHERE user_id = ?";
            }
            sql += " ORDER BY user_id";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (filterById != null) {
                    ps.setInt(1, filterById);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        userIds.add(rs.getInt("user_id"));
                        tableModel.addRow(new Object[]{
                            rs.getInt("user_id"),
                            rs.getString("role"),
                            rs.getString("username"),
                            rs.getString("name"),
                            rs.getString("status")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void loadLoggedInUserDetails() {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT user_id, username, name FROM users WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("user_id");
                        String username = nullToEmpty(rs.getString("username"));
                        String name = nullToEmpty(rs.getString("name"));
                        UserID.setText("User ID: " + id);
                        UserName.setText("Username: " + username);
                        Name.setText("Name: " + name);
                        Email.setText("");
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load current user info: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }


    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        EditUserPanel.setCursor(handCursor);
        EditUserPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != ViewMode.USERS) {
                    currentMode = ViewMode.USERS;
                    tableModel.setColumnIdentifiers(USER_COLUMNS);
                    loadUsersFromDb(null);
                }
                performEditUser();
            }
        });

        AddUserPanel.setCursor(handCursor);
        AddUserPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != ViewMode.USERS) {
                    currentMode = ViewMode.USERS;
                    tableModel.setColumnIdentifiers(USER_COLUMNS);
                    loadUsersFromDb(null);
                }
                performAddUser();
            }
        });

        RemoveUserPanel.setCursor(handCursor);
        RemoveUserPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != ViewMode.USERS) {
                    currentMode = ViewMode.USERS;
                    tableModel.setColumnIdentifiers(USER_COLUMNS);
                    loadUsersFromDb(null);
                }
                performDeleteUser();
            }
        });

        ApproveUserPanel.setCursor(handCursor);
        ApproveUserPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentMode != ViewMode.USERS) {
                    currentMode = ViewMode.USERS;
                    tableModel.setColumnIdentifiers(USER_COLUMNS);
                    loadUsersFromDb(null);
                }
                performApproveUser();
            }
        });

        jTableBookings.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && jTableBookings.getSelectedRow() >= 0 && currentMode == ViewMode.USERS) {
                    performEditUser();
                }
            }
        });
    }

    private void loadParkingTransactions() {
        tableModel.setRowCount(0);
        userIds.clear();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT transaction_id, vehicle_id, slot_id, time_in, time_out, parking_fee, status FROM parking_transactions ORDER BY transaction_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("transaction_id"),
                        rs.getInt("vehicle_id"),
                        rs.getInt("slot_id"),
                        rs.getString("time_in"),
                        rs.getString("time_out"),
                        rs.getObject("parking_fee"),
                        rs.getString("status")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load transactions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void loadParkingStats() {
        tableModel.setRowCount(0);
        userIds.clear();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            int total = 0, available = 0, occupied = 0;
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT status, COUNT(*) AS cnt FROM parking_slots GROUP BY status")) {
                while (rs.next()) {
                    total += rs.getInt("cnt");
                    String st = rs.getString("status");
                    if ("Available".equalsIgnoreCase(st)) available += rs.getInt("cnt");
                    else if ("Occupied".equalsIgnoreCase(st)) occupied += rs.getInt("cnt");
                }
            }
            int activeTx = 0;
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS cnt FROM parking_transactions WHERE status = 'Active'")) {
                if (rs.next()) activeTx = rs.getInt("cnt");
            }
            tableModel.setColumnIdentifiers(new String[]{"Metric", "Count"});
            tableModel.addRow(new Object[]{"Total slots", total});
            tableModel.addRow(new Object[]{"Available", available});
            tableModel.addRow(new Object[]{"Occupied", occupied});
            tableModel.addRow(new Object[]{"Active transactions", activeTx});
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load reports: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void loadPayments() {
        tableModel.setRowCount(0);
        userIds.clear();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT payment_id, transaction_id, cashier_id, amount, payment_date FROM payments ORDER BY payment_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("payment_id"),
                        rs.getInt("transaction_id"),
                        rs.getInt("cashier_id"),
                        rs.getDouble("amount"),
                        rs.getString("payment_date")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load payments: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performAddUser() {
        if (currentMode != ViewMode.USERS) {
            JOptionPane.showMessageDialog(this, "Add is only available in Users view.", "Invalid Action", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, username, password, role) VALUES (?, ?, ?, ?)")) {
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
        if (currentMode != ViewMode.USERS) {
            JOptionPane.showMessageDialog(this, "Edit is only available in Users view.", "Invalid Action", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int row = jTableBookings.getSelectedRow();
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
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setOpaque(false);
        panel.add(new javax.swing.JLabel("Role:"));
        panel.add(roleCombo);
        panel.add(new javax.swing.JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new javax.swing.JLabel("Name:"));
        panel.add(nameField);
        panel.add(new javax.swing.JLabel("New password (optional):"));
        panel.add(passwordField);
        if (StyledDialog.showFormConfirm(this, panel, "Edit User") != JOptionPane.OK_OPTION) {
            return;
        }
        String role = (String) roleCombo.getSelectedItem();
        String username = usernameField.getText().trim();
        String name = nameField.getText().trim();
        String newPassword = passwordField.getText().trim();
        if (role.isEmpty() || username.isEmpty() || name.isEmpty()) {
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
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET role = ?, username = ?, name = ?, password = ? WHERE user_id = ?")) {
                    ps.setString(1, role);
                    ps.setString(2, username);
                    ps.setString(3, name);
                    ps.setString(4, PasswordUtil.hashPassword(newPassword));
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET role = ?, username = ?, name = ? WHERE user_id = ?")) {
                    ps.setString(1, role);
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
        if (currentMode != ViewMode.USERS) {
            JOptionPane.showMessageDialog(this, "Delete is only available in Users view.", "Invalid Action", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int row = jTableBookings.getSelectedRow();
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

    private void performApproveUser() {
        if (currentMode != ViewMode.USERS) {
            JOptionPane.showMessageDialog(this, "Approve is only available in Users view.", "Invalid Action", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int row = jTableBookings.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Please select a user to approve.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String status = String.valueOf(tableModel.getValueAt(row, 4));
        if (!"pending".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Selected user is not pending approval.", "Invalid Action", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET status = 'approved' WHERE user_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            loadUsersFromDb(null);
            JOptionPane.showMessageDialog(this, "User approved. They can now log in.", "Approved", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to approve user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTableBookings = new javax.swing.JTable();
        EditUserPanel = new javax.swing.JPanel();
        EditUserText = new javax.swing.JLabel();
        AddUserPanel = new javax.swing.JPanel();
        AddUserText = new javax.swing.JLabel();
        RemoveUserPanel = new javax.swing.JPanel();
        RemoveUserText = new javax.swing.JLabel();
        ApproveUserPanel = new javax.swing.JPanel();
        ApproveUserText = new javax.swing.JLabel();
        UserOutPanel = new javax.swing.JPanel();
        UserPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        UserName = new javax.swing.JLabel();
        Name = new javax.swing.JLabel();
        UserID = new javax.swing.JLabel();
        Email = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(790, 415));

        mainPanel.setBackground(new java.awt.Color(15, 23, 42));
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        javax.swing.GroupLayout jScrollPane1Layout = new javax.swing.GroupLayout(jScrollPane1.getViewport());
        jScrollPane1.getViewport().setLayout(jScrollPane1Layout);
        jScrollPane1Layout.setHorizontalGroup(
            jScrollPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTableBookings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jScrollPane1Layout.setVerticalGroup(
            jScrollPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTableBookings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        mainPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 80, 460, 360));

        EditUserPanel.setBackground(new java.awt.Color(102, 102, 102));

        EditUserText.setBackground(new java.awt.Color(51, 51, 51));
        EditUserText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        EditUserText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EditUserText.setText("EDIT USER");

        javax.swing.GroupLayout EditUserPanelLayout = new javax.swing.GroupLayout(EditUserPanel);
        EditUserPanel.setLayout(EditUserPanelLayout);
        EditUserPanelLayout.setHorizontalGroup(
            EditUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(EditUserText, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
        );
        EditUserPanelLayout.setVerticalGroup(
            EditUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, EditUserPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(EditUserText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanel.add(EditUserPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 110, 60));

        AddUserPanel.setBackground(new java.awt.Color(102, 102, 102));

        AddUserText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        AddUserText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        AddUserText.setText("ADD USER");

        javax.swing.GroupLayout AddUserPanelLayout = new javax.swing.GroupLayout(AddUserPanel);
        AddUserPanel.setLayout(AddUserPanelLayout);
        AddUserPanelLayout.setHorizontalGroup(
            AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(AddUserText, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
        );
        AddUserPanelLayout.setVerticalGroup(
            AddUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, AddUserPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(AddUserText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanel.add(AddUserPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 10, 110, 60));

        RemoveUserPanel.setBackground(new java.awt.Color(102, 102, 102));

        RemoveUserText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RemoveUserText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        RemoveUserText.setText("REMOVE USER");

        javax.swing.GroupLayout RemoveUserPanelLayout = new javax.swing.GroupLayout(RemoveUserPanel);
        RemoveUserPanel.setLayout(RemoveUserPanelLayout);
        RemoveUserPanelLayout.setHorizontalGroup(
            RemoveUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(RemoveUserText, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
        );
        RemoveUserPanelLayout.setVerticalGroup(
            RemoveUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RemoveUserPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(RemoveUserText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanel.add(RemoveUserPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 10, 110, 60));

        ApproveUserPanel.setBackground(new java.awt.Color(102, 102, 102));
        ApproveUserText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        ApproveUserText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        ApproveUserText.setText("APPROVE USER");
        javax.swing.GroupLayout ApproveUserPanelLayout = new javax.swing.GroupLayout(ApproveUserPanel);
        ApproveUserPanel.setLayout(ApproveUserPanelLayout);
        ApproveUserPanelLayout.setHorizontalGroup(
            ApproveUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ApproveUserText, javax.swing.GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
        );
        ApproveUserPanelLayout.setVerticalGroup(
            ApproveUserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ApproveUserText, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        mainPanel.add(ApproveUserPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 10, 110, 60));

        UserOutPanel.setBackground(new java.awt.Color(102, 102, 102));

        UserPanel.setBackground(new java.awt.Color(153, 153, 153));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/icons8-profile-100.png"))); // NOI18N

        javax.swing.GroupLayout UserPanelLayout = new javax.swing.GroupLayout(UserPanel);
        UserPanel.setLayout(UserPanelLayout);
        UserPanelLayout.setHorizontalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        UserPanelLayout.setVerticalGroup(
            UserPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
        );

        UserName.setText("Username:");

        Name.setText("Name:");

        UserID.setText("User ID:");

        Email.setText("Email:");

        javax.swing.GroupLayout UserOutPanelLayout = new javax.swing.GroupLayout(UserOutPanel);
        UserOutPanel.setLayout(UserOutPanelLayout);
        UserOutPanelLayout.setHorizontalGroup(
            UserOutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserOutPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(UserOutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(UserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(UserOutPanelLayout.createSequentialGroup()
                        .addGroup(UserOutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(UserName)
                            .addComponent(Name)
                            .addComponent(UserID)
                            .addComponent(Email))
                        .addGap(0, 68, Short.MAX_VALUE)))
                .addContainerGap())
        );
        UserOutPanelLayout.setVerticalGroup(
            UserOutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(UserOutPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(UserPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(UserName)
                .addGap(18, 18, 18)
                .addComponent(Name)
                .addGap(19, 19, 19)
                .addComponent(UserID)
                .addGap(18, 18, 18)
                .addComponent(Email)
                .addContainerGap(102, Short.MAX_VALUE))
        );

        mainPanel.add(UserOutPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, 140, 360));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 695, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel AddUserPanel;
    private javax.swing.JLabel AddUserText;
    private javax.swing.JPanel ApproveUserPanel;
    private javax.swing.JLabel ApproveUserText;
    private javax.swing.JPanel EditUserPanel;
    private javax.swing.JLabel EditUserText;
    private javax.swing.JLabel Email;
    private javax.swing.JLabel Name;
    private javax.swing.JPanel RemoveUserPanel;
    private javax.swing.JLabel RemoveUserText;
    private javax.swing.JLabel UserID;
    private javax.swing.JLabel UserName;
    private javax.swing.JPanel UserOutPanel;
    private javax.swing.JPanel UserPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableBookings;
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables
}
