package Cashier;

import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.PasswordUtil;
import User.InternalPageFrame;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class CashierDash extends InternalPageFrame {

    private final int currentUserId;

    public CashierDash(int currentUserId) {
        this.currentUserId = currentUserId;
        initComponents();
        applyModernTheme();
        loadCashierInfo();
        setupPanelListeners();
    }

    private void applyModernTheme() {
        // Match UserDash look: light content bg + white card with avatar on the left
        mainPanel.setBackground(AppTheme.CONTENT_BG);

        // Rebuild profile card so avatar is on the left and text is to the right, like UserDash
        UserOutPanel.removeAll();
        UserOutPanel.setBackground(AppTheme.CARD_BG);
        UserOutPanel.setBorder(javax.swing.BorderFactory.createLineBorder(AppTheme.BORDER));
        UserOutPanel.setLayout(new java.awt.BorderLayout(12, 0));

        // Avatar panel on the left
        UserPanel.setBackground(AppTheme.CARD_BG);
        UserOutPanel.add(UserPanel, java.awt.BorderLayout.WEST);

        // Text labels stacked vertically on the right, centered vertically
        javax.swing.JPanel infoPanel = new javax.swing.JPanel();
        infoPanel.setBackground(AppTheme.CARD_BG);
        infoPanel.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.CENTER;

        javax.swing.JPanel labelsColumn = new javax.swing.JPanel();
        labelsColumn.setBackground(AppTheme.CARD_BG);
        labelsColumn.setLayout(new javax.swing.BoxLayout(labelsColumn, javax.swing.BoxLayout.Y_AXIS));
        labelsColumn.add(UserName);
        labelsColumn.add(javax.swing.Box.createVerticalStrut(4)); // 1-space vertical gap
        labelsColumn.add(Name);
        labelsColumn.add(javax.swing.Box.createVerticalStrut(4)); // 1-space vertical gap
        labelsColumn.add(UserID);
        labelsColumn.add(javax.swing.Box.createVerticalStrut(4)); // 1-space vertical gap
        labelsColumn.add(Email);

        infoPanel.add(labelsColumn, gbc);
        UserOutPanel.add(infoPanel, java.awt.BorderLayout.CENTER);

        // Edit password button styled like UserDash nav buttons
        EditPasswordPanel.setBackground(AppTheme.NAV_BTN_BG);
        EditUserText.setForeground(AppTheme.TEXT_ON_NAV);
        EditUserText.setFont(AppTheme.FONT_NAV);

        java.awt.Color text  = AppTheme.TEXT_PRIMARY;
        java.awt.Color muted = AppTheme.TEXT_MUTED;
        java.awt.Font boldFont = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12);
        UserName.setForeground(text);
        UserName.setFont(boldFont);
        Name.setForeground(text);
        Name.setFont(boldFont);
        UserID.setForeground(text);
        UserID.setFont(boldFont);
        Email.setForeground(muted);
    }

    private void loadCashierInfo() {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT user_id, username, name FROM users WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("user_id");
                        String username = rs.getString("username");
                        String name = rs.getString("name");
                        UserID.setText("User ID: " + id);
                        UserName.setText("Username: " + username);
                        Name.setText("Name: " + name);
                        Email.setText("");
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load cashier info: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void setupPanelListeners() {
        // Hook up Edit Password like UserDash
        EditPasswordPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        EditPasswordPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                performChangePassword();
            }
        });
    }

    private void performChangePassword() {
        javax.swing.JTextField currentPasswordField = new javax.swing.JTextField(20);
        javax.swing.JTextField newPasswordField = new javax.swing.JTextField(20);
        javax.swing.JTextField confirmField = new javax.swing.JTextField(20);
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(3, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Current password:"));
        panel.add(currentPasswordField);
        panel.add(new javax.swing.JLabel("New password:"));
        panel.add(newPasswordField);
        panel.add(new javax.swing.JLabel("Confirm new password:"));
        panel.add(confirmField);
        if (javax.swing.JOptionPane.showConfirmDialog(this, panel, "Change Password", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE) != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText().trim();
        String confirm = confirmField.getText();
        if (newPassword.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "New password cannot be empty.", "Change Password", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirm)) {
            javax.swing.JOptionPane.showMessageDialog(this, "New password and confirmation do not match.", "Change Password", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        java.sql.Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT password FROM users WHERE user_id = ?")) {
                ps.setInt(1, currentUserId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !PasswordUtil.verifyPassword(currentPassword, rs.getString("password"))) {
                        javax.swing.JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Change Password", javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?")) {
                ps.setString(1, PasswordUtil.hashPassword(newPassword));
                ps.setInt(2, currentUserId);
                ps.executeUpdate();
            }
            javax.swing.JOptionPane.showMessageDialog(this, "Password changed successfully.", "Change Password", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this, "Failed to change password: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
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
        UserOutPanel = new javax.swing.JPanel();
        UserPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        UserName = new javax.swing.JLabel();
        Name = new javax.swing.JLabel();
        UserID = new javax.swing.JLabel();
        Email = new javax.swing.JLabel();

        setPreferredSize(new java.awt.Dimension(790, 415));

        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        EditPasswordPanel.setBackground(new java.awt.Color(45, 55, 72));

        EditUserText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        EditUserText.setForeground(new java.awt.Color(255, 255, 255));
        EditUserText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EditUserText.setText("EDIT PASSWORD");

        javax.swing.GroupLayout EditPasswordPanelLayout = new javax.swing.GroupLayout(EditPasswordPanel);
        EditPasswordPanel.setLayout(EditPasswordPanelLayout);
        EditPasswordPanelLayout.setHorizontalGroup(
            EditPasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(EditUserText, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
        );
        EditPasswordPanelLayout.setVerticalGroup(
            EditPasswordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(EditUserText, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        mainPanel.add(EditPasswordPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 30, 170, 40));

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

        mainPanel.add(UserOutPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, 350, 150));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 610, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 453, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Email;
    private javax.swing.JPanel EditPasswordPanel;
    private javax.swing.JLabel EditUserText;
    private javax.swing.JLabel Name;
    private javax.swing.JLabel UserID;
    private javax.swing.JLabel UserName;
    private javax.swing.JPanel UserOutPanel;
    private javax.swing.JPanel UserPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel mainPanel;
    // End of variables declaration//GEN-END:variables
}

