package Main;

import Configuration.ConnectionConfig;
import Configuration.PasswordUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import org.netbeans.lib.awtextra.AbsoluteConstraints;

public class RegisterFrame extends javax.swing.JFrame {

    public RegisterFrame() {
        initComponents();
        applyRegisterStyles();
        setupPanelListeners();
        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                UsernameField.requestFocusInWindow();
            }
        });
    }

    private void applyRegisterStyles() {
        // Match new login aesthetic: deep navy + teal, keep layout/labels as-is
        Color bgOuter     = new Color(14, 22, 36);
        Color cardBg      = new Color(18, 32, 52);
        Color cardBorder  = new Color(33, 56, 89);
        Color accent      = new Color(0, 163, 173);
        Color accentHover = new Color(0, 190, 200);
        Color textPrimary = new Color(236, 244, 255);
        Color textMuted   = new Color(139, 152, 176);
        Color inputBg     = new Color(14, 22, 36);
        Color inputFg     = new Color(224, 232, 255);
        Color borderLight = new Color(63, 82, 118);

        MainFrame.setBackground(bgOuter);
        jPanel3.setBackground(cardBg);
        jPanel3.setBorder(new CompoundBorder(
                new LineBorder(cardBorder, 1, true),
                new EmptyBorder(28, 32, 28, 32)));

        Welcoming.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 20));
        Welcoming.setForeground(textPrimary);
        Welcoming1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        Welcoming1.setForeground(textMuted);
        Welcoming2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        Welcoming2.setForeground(textMuted);

        EnterUsernameText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        EnterUsernameText.setForeground(textMuted);
        EnterEmailText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        EnterEmailText.setForeground(textMuted);
        EnterNameTest.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        EnterNameTest.setForeground(textMuted);
        EnterPasswordText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        EnterPasswordText.setForeground(textMuted);

        // Re-position labels so each sits clearly above its field (same pattern as login)
        jPanel3.remove(EnterUsernameText);
        jPanel3.add(EnterUsernameText, new AbsoluteConstraints(0, 120, 420, 20));
        jPanel3.remove(EnterEmailText);
        jPanel3.add(EnterEmailText, new AbsoluteConstraints(0, 180, 420, 20));
        jPanel3.remove(EnterNameTest);
        jPanel3.add(EnterNameTest, new AbsoluteConstraints(0, 240, 420, 20));
        jPanel3.remove(EnterPasswordText);
        jPanel3.add(EnterPasswordText, new AbsoluteConstraints(0, 300, 420, 20));

        // Match login form field size / spacing for consistent look
        int fieldW = 340;
        int fieldH = 32;
        int marginX = 40;

        javax.swing.JTextField[] fields = { UsernameField, EmailField, NameField, PasswordField };
        int[] fieldY = { 150, 210, 270, 330 };
        for (int i = 0; i < fields.length; i++) {
            javax.swing.JTextField f = fields[i];
            jPanel3.remove(f);
            jPanel3.add(f, new AbsoluteConstraints(marginX, fieldY[i], fieldW, fieldH));
            f.setBackground(inputBg);
            f.setForeground(inputFg);
            f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            f.setCaretColor(accent);
            f.setBorder(new CompoundBorder(new LineBorder(borderLight, 1, true), new EmptyBorder(8, 10, 8, 10)));
            f.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        }

        // Style REGISTER button to match new LOGIN button
        LoginBtnPanel.setBackground(accent);
        LoginBtnPanel.setBorder(new EmptyBorder(10, 18, 10, 18));
        LoginText.setForeground(Color.WHITE);
        LoginText.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        LoginBtnPanel.setLayout(new BorderLayout());
        LoginBtnPanel.removeAll();
        LoginText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LoginText.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        LoginBtnPanel.add(LoginText, BorderLayout.CENTER);
        // Ensure consistent size and centered under fields
        jPanel3.remove(LoginBtnPanel);
        int btnW = 180;
        int btnH = 44;
        int btnX = (420 - btnW) / 2;
        int btnY = 380; // move REGISTER button slightly down
        jPanel3.add(LoginBtnPanel, new AbsoluteConstraints(btnX, btnY, btnW, btnH));

        // "Already have an account? Log in" – inline, like LOGIN screen
        javax.swing.JPanel alreadyHavePanel = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 4, 0));
        alreadyHavePanel.setBackground(cardBg);
        alreadyHavePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        javax.swing.JLabel alreadyHaveLbl = new javax.swing.JLabel("Already have an account?");
        alreadyHaveLbl.setForeground(textMuted);
        alreadyHaveLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        javax.swing.JLabel loginLinkLbl = new javax.swing.JLabel(" Log in");
        loginLinkLbl.setForeground(accent);
        loginLinkLbl.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        loginLinkLbl.setCursor(new Cursor(Cursor.HAND_CURSOR));

        alreadyHavePanel.add(alreadyHaveLbl);
        alreadyHavePanel.add(loginLinkLbl);
        alreadyHavePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
                Mainframe m = new Mainframe();
                m.setLocationRelativeTo(null);
                m.setVisible(true);
            }
        });
        loginLinkLbl.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
                Mainframe m = new Mainframe();
                m.setLocationRelativeTo(null);
                m.setVisible(true);
            }
        });
        int cardW = 420;
        int linkPanelW = 220;
        int linkY = 430; // keep text a bit below the moved button
        jPanel3.add(alreadyHavePanel, new AbsoluteConstraints((cardW - linkPanelW) / 2, linkY, linkPanelW, 22));

        // Center the registration card in the window
        MainFrame.remove(jPanel3);
        MainFrame.setLayout(new BorderLayout());
        MainFrame.add(jPanel3, BorderLayout.CENTER);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        LoginBtnPanel.setCursor(handCursor);
        LoginBtnPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                performRegister();
            }
        });
    }

    private void performRegister() {
        String username = safeTrim(UsernameField.getText());
        String name = safeTrim(NameField.getText());
        String password = safeTrim(new String(PasswordField.getPassword()));

        if (username.isEmpty() || name.isEmpty() || password.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Please fill in username, name, and password.",
                "Registration",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (username.length() < 2) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Username must be at least 2 characters.",
                "Registration",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length() < 4) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Password must be at least 4 characters.",
                "Registration",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement checkUser = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
                checkUser.setString(1, username);
                try (ResultSet rs = checkUser.executeQuery()) {
                    if (rs.next()) {
                        javax.swing.JOptionPane.showMessageDialog(this,
                            "Username already exists. Please choose another.",
                            "Registration",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            String hashed = PasswordUtil.hashPassword(password);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (name, username, password, role, status) VALUES (?, ?, ?, 'user', 'pending')")) {
                ps.setString(1, name);
                ps.setString(2, username);
                ps.setString(3, hashed);
                ps.executeUpdate();
            }
            javax.swing.JOptionPane.showMessageDialog(this,
                "Registration submitted. Your account is pending approval. You can log in once an administrator approves it.",
                "Registration",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
            dispose();
            Mainframe mainframe = new Mainframe();
            mainframe.setLocationRelativeTo(null);
            mainframe.setVisible(true);
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Database error: " + e.getMessage(),
                "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MainFrame = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        Welcoming = new javax.swing.JLabel();
        Welcoming1 = new javax.swing.JLabel();
        Welcoming2 = new javax.swing.JLabel();
        EnterUsernameText = new javax.swing.JLabel();
        UsernameField = new javax.swing.JTextField();
        EnterEmailText = new javax.swing.JLabel();
        EmailField = new javax.swing.JTextField();
        EnterNameTest = new javax.swing.JLabel();
        NameField = new javax.swing.JTextField();
        EnterPasswordText = new javax.swing.JLabel();
        PasswordField = new javax.swing.JPasswordField();
        LoginBtnPanel = new javax.swing.JPanel();
        LoginText = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        MainFrame.setBackground(new java.awt.Color(26, 26, 46)); // same dark background as login
        MainFrame.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel3.setBackground(new java.awt.Color(22, 33, 62)); // same card color as login
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        Welcoming.setBackground(new java.awt.Color(255, 255, 255));
        Welcoming.setFont(new java.awt.Font("Tahoma", 1, 16)); // NOI18N
        Welcoming.setForeground(new java.awt.Color(255, 255, 255));
        Welcoming.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Welcoming.setText("Welcome to Parking Slotwise");
        jPanel3.add(Welcoming, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 30, 430, -1));

        Welcoming1.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        Welcoming1.setForeground(new java.awt.Color(255, 255, 255));
        Welcoming1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Welcoming1.setText("Please fill out the form to complete");
        jPanel3.add(Welcoming1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 70, 430, -1));

        Welcoming2.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        Welcoming2.setForeground(new java.awt.Color(255, 255, 255));
        Welcoming2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Welcoming2.setText("Registration, Thank you!");
        jPanel3.add(Welcoming2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 90, 430, -1));

        EnterUsernameText.setFont(new java.awt.Font("Segoe UI", 1, 12)); // match login label
        EnterUsernameText.setForeground(new java.awt.Color(160, 174, 192));
        EnterUsernameText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EnterUsernameText.setText("USERNAME");
        jPanel3.add(EnterUsernameText, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 120, 430, 20));

        UsernameField.setBackground(new java.awt.Color(255, 255, 255));
        UsernameField.setForeground(new java.awt.Color(45, 55, 72));
        UsernameField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        UsernameField.setBorder(null);
        UsernameField.setCaretColor(new java.awt.Color(255, 255, 255));
        UsernameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UsernameFieldActionPerformed(evt);
            }
        });
        jPanel3.add(UsernameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 140, 240, 20));

        EnterEmailText.setBackground(new java.awt.Color(255, 255, 255));
        EnterEmailText.setForeground(new java.awt.Color(160, 174, 192));
        EnterEmailText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EnterEmailText.setText("EMAIL");
        jPanel3.add(EnterEmailText, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 170, 430, 20));

        EmailField.setBackground(new java.awt.Color(255, 255, 255));
        EmailField.setForeground(new java.awt.Color(45, 55, 72));
        EmailField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        EmailField.setBorder(null);
        EmailField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EmailFieldActionPerformed(evt);
            }
        });
        jPanel3.add(EmailField, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 190, 240, 20));

        EnterNameTest.setForeground(new java.awt.Color(160, 174, 192));
        EnterNameTest.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EnterNameTest.setText("NAME");
        jPanel3.add(EnterNameTest, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 220, 430, 20));

        NameField.setBackground(new java.awt.Color(255, 255, 255));
        NameField.setForeground(new java.awt.Color(45, 55, 72));
        NameField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        NameField.setBorder(null);
        NameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NameFieldActionPerformed(evt);
            }
        });
        jPanel3.add(NameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 240, 240, 20));

        EnterPasswordText.setForeground(new java.awt.Color(160, 174, 192));
        EnterPasswordText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        EnterPasswordText.setText("PASSWORD");
        jPanel3.add(EnterPasswordText, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 270, 430, 20));

        PasswordField.setBackground(new java.awt.Color(255, 255, 255));
        PasswordField.setForeground(new java.awt.Color(45, 55, 72));
        PasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        PasswordField.setBorder(null);
        PasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PasswordFieldActionPerformed(evt);
            }
        });
        jPanel3.add(PasswordField, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 300, 240, 20));

        LoginBtnPanel.setBackground(new java.awt.Color(204, 204, 204));

        LoginText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        LoginText.setForeground(new java.awt.Color(255, 255, 255));
        LoginText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LoginText.setText("REGISTER");
        LoginText.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout LoginBtnPanelLayout = new javax.swing.GroupLayout(LoginBtnPanel);
        LoginBtnPanel.setLayout(LoginBtnPanelLayout);
        LoginBtnPanelLayout.setHorizontalGroup(
            LoginBtnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(LoginText, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
        );
        LoginBtnPanelLayout.setVerticalGroup(
            LoginBtnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(LoginText, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        jPanel3.add(LoginBtnPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 340, 180, 40));

        MainFrame.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 430, 440));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainFrame, javax.swing.GroupLayout.PREFERRED_SIZE, 454, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainFrame, javax.swing.GroupLayout.PREFERRED_SIZE, 459, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void UsernameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UsernameFieldActionPerformed
    }//GEN-LAST:event_UsernameFieldActionPerformed

    private void PasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PasswordFieldActionPerformed
    }//GEN-LAST:event_PasswordFieldActionPerformed

    private void EmailFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EmailFieldActionPerformed
    }//GEN-LAST:event_EmailFieldActionPerformed

    private void NameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NameFieldActionPerformed
    }//GEN-LAST:event_NameFieldActionPerformed

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(RegisterFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new RegisterFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField EmailField;
    private javax.swing.JLabel EnterEmailText;
    private javax.swing.JLabel EnterNameTest;
    private javax.swing.JLabel EnterPasswordText;
    private javax.swing.JLabel EnterUsernameText;
    private javax.swing.JPanel LoginBtnPanel;
    private javax.swing.JLabel LoginText;
    private javax.swing.JPanel MainFrame;
    private javax.swing.JTextField NameField;
    private javax.swing.JPasswordField PasswordField;
    private javax.swing.JTextField UsernameField;
    private javax.swing.JLabel Welcoming;
    private javax.swing.JLabel Welcoming1;
    private javax.swing.JLabel Welcoming2;
    private javax.swing.JPanel jPanel3;
    // End of variables declaration//GEN-END:variables
}
