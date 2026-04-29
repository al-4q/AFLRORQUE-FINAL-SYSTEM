package Main;

import Configuration.ConnectionConfig;
import Configuration.PasswordUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.awt.Rectangle;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Mainframe extends javax.swing.JFrame {
        
    Color navcolor = new Color(153,153,255);
    Color bodycolor = new Color(255,255,255);
    Color staycolor = new Color(153,153,255);
    
    public Mainframe() {
        initComponents();
        applyLoginStyles();
        setupPanelListeners();
        pack();
        setLocationRelativeTo(null);
        centerLoginContentsLater();
        // Hide bottom REGISTER strip on login page (no extra button)
        RegisterPanel.setVisible(false);
    }

    private void applyLoginStyles() {
        // New aesthetic: deep navy + teal, keeping layout/labels the same
        Color bgOuter     = new Color(14, 22, 36);
        Color cardBg      = new Color(18, 32, 52);
        Color cardBorder  = new Color(33, 56, 89);
        Color accent      = new Color(0, 163, 173);
        Color accentHover = new Color(0, 190, 200);
        Color textPrimary = new Color(236, 244, 255);
        Color textMuted   = new Color(139, 152, 176);
        Color inputBg     = new Color(14, 22, 36);
        Color inputFg     = new Color(224, 232, 255);
        Color inputBorder = new Color(63, 82, 118);

        MainFrame.setBackground(bgOuter);
        jPanel3.setBackground(cardBg);
        jPanel3.setBorder(new CompoundBorder(
                new LineBorder(cardBorder, 1, true),
                new EmptyBorder(32, 36, 32, 36)));

        Welcoming.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 22));
        Welcoming.setForeground(textPrimary);

        UsernameText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        UsernameText.setForeground(textMuted);
        PasswordText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        PasswordText.setForeground(textMuted);

        // Inputs: keep absolute positions, just restyle
        UsernameField.setBackground(inputBg);
        UsernameField.setForeground(inputFg);
        UsernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        UsernameField.setCaretColor(accent);
        UsernameField.setBorder(new CompoundBorder(
                new LineBorder(inputBorder, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        UsernameField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        PasswordField.setBackground(inputBg);
        PasswordField.setForeground(inputFg);
        PasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        PasswordField.setCaretColor(accent);
        PasswordField.setBorder(new CompoundBorder(
                new LineBorder(inputBorder, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        PasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        // Login button: teal, flat, hoverable. Form file pins the panel at 130×30; a 10+10 vertical
        // border would leave ~10px for text — too small for Semibold 14. Size from font metrics.
        LoginBtnPanel.setBackground(accent);
        Font loginBtnFont = new Font("Segoe UI Semibold", Font.PLAIN, 14);
        LoginText.setForeground(Color.WHITE);
        LoginText.setFont(loginBtnFont);
        LoginText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LoginText.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        int padV = 10;
        int padH = 32;
        LoginBtnPanel.setBorder(new EmptyBorder(padV, padH, padV, padH));
        LoginBtnPanel.setLayout(new BorderLayout());
        LoginBtnPanel.removeAll();
        LoginBtnPanel.add(LoginText, BorderLayout.CENTER);
        LoginBtnPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        int textLineH = LoginText.getFontMetrics(loginBtnFont).getHeight();
        int btnH = Math.max(42, textLineH + 2 * padV);
        int btnW = Math.max(160, LoginText.getPreferredSize().width + 2 * padH);
        Rectangle loginBounds = LoginBtnPanel.getBounds();
        int centerX = loginBounds.x + loginBounds.width / 2;
        LoginBtnPanel.setBounds(centerX - btnW / 2, loginBounds.y, btnW, btnH);

        LoginBtnPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                LoginBtnPanel.setBackground(accentHover);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                LoginBtnPanel.setBackground(accent);
            }
        });

        RegisterPanel.setBackground(cardBg);
        jLabel1.setForeground(accent);
        jLabel1.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        Extra.setForeground(textMuted);
        Extra.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Center the login card in the window
        MainFrame.remove(jPanel3);
        MainFrame.setLayout(new BorderLayout());
        MainFrame.add(jPanel3, BorderLayout.CENTER);
    }

    private void centerLoginContentsLater() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            centerLoginContents();
            // Keep centered if the window is resized (or shown later)
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    centerLoginContents();
                }
            });
        });
    }

    private void centerLoginContents() {
        // jPanel3 uses AbsoluteLayout; we shift the whole block to vertically center.
        Component[] items = new Component[] {
            Welcoming,
            UsernameText,
            UsernameField,
            PasswordText,
            PasswordField,
            LoginBtnPanel,
            Extra,
            RegisterPanel
        };

        Rectangle group = null;
        for (Component c : items) {
            if (c == null || !c.isShowing() && jPanel3.getWidth() == 0) continue;
            Rectangle b = c.getBounds();
            if (b.width <= 0 || b.height <= 0) continue;
            group = (group == null) ? new Rectangle(b) : group.union(b);
        }
        if (group == null) return;

        int panelH = jPanel3.getHeight();
        if (panelH <= 0) return;

        int desiredTop = Math.max(0, (panelH - group.height) / 2);
        int deltaY = desiredTop - group.y;

        // Clamp so we don't move content outside the card
        int minDelta = -group.y;
        int maxDelta = panelH - (group.y + group.height);
        if (deltaY < minDelta) deltaY = minDelta;
        if (deltaY > maxDelta) deltaY = maxDelta;

        if (deltaY == 0) return;

        for (Component c : items) {
            Rectangle b = c.getBounds();
            c.setBounds(b.x, b.y + deltaY, b.width, b.height);
        }
        jPanel3.revalidate();
        jPanel3.repaint();
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        LoginBtnPanel.setCursor(handCursor);
        LoginBtnPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                performLogin();
            }
        });

        RegisterPanel.setCursor(handCursor);
        RegisterPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegisterFrame();
            }
        });
        Extra.setCursor(handCursor);
        Extra.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegisterFrame();
            }
        });
    }

    private void performLogin() {
        String username = UsernameField.getText();
        String password = new String(PasswordField.getPassword());
        if (username != null) username = username.trim();
        else username = "";
        if (password != null) password = password.trim();
        else password = "";
        if (username.isEmpty() || password.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Please enter username and password.",
                "Login",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            int userId = -1;
            String role = "user";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id, password, COALESCE(role, 'user') AS role, COALESCE(status, 'approved') AS status FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        javax.swing.JOptionPane.showMessageDialog(this,
                            "Invalid username or password.",
                            "Login Failed",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String storedHash = rs.getString("password");
                    if (!PasswordUtil.verifyPassword(password, storedHash)) {
                        javax.swing.JOptionPane.showMessageDialog(this,
                            "Invalid username or password.",
                            "Login Failed",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String userStatus = rs.getString("status");
                    if (userStatus != null) {
                        String s = userStatus.trim().toLowerCase();
                        if ("pending".equals(s)) {
                            javax.swing.JOptionPane.showMessageDialog(this,
                                "Your account is pending approval. Please wait for an administrator to approve it.",
                                "Account Pending",
                                javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        if ("rejected".equals(s)) {
                            javax.swing.JOptionPane.showMessageDialog(this,
                                "Your registration was rejected. Contact an administrator if you believe this is an error.",
                                "Account Rejected",
                                javax.swing.JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                    userId = rs.getInt("user_id");
                    role = rs.getString("role");
                    if (role == null) role = "user";
                }
            }
            final String roleFinal = role.trim();
            final int uid = userId;
            dispose();
            MainPage mainPage = new MainPage(roleFinal, uid);
            mainPage.setLocationRelativeTo(null);
            mainPage.setVisible(true);
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                "Database error: " + e.getMessage(),
                "Error",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void openRegisterFrame() {
        dispose();
        RegisterFrame registerFrame = new RegisterFrame();
        registerFrame.setLocationRelativeTo(null);
        registerFrame.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MainFrame = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        UsernameText = new javax.swing.JLabel();
        UsernameField = new javax.swing.JTextField();
        PasswordText = new javax.swing.JLabel();
        PasswordField = new javax.swing.JPasswordField();
        LoginBtnPanel = new javax.swing.JPanel();
        LoginText = new javax.swing.JLabel();
        RegisterPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        Welcoming = new javax.swing.JLabel();
        Extra = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        setPreferredSize(new java.awt.Dimension(444, 450));
        setResizable(false);

        MainFrame.setBackground(new java.awt.Color(14, 22, 36));
        MainFrame.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel3.setBackground(new java.awt.Color(18, 32, 52));
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        UsernameText.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        UsernameText.setBackground(new java.awt.Color(14, 22, 36));
        UsernameText.setForeground(new java.awt.Color(224, 232, 255));
        UsernameText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        UsernameText.setText("USERNAME");
        jPanel3.add(UsernameText, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 90, 420, 20));

        UsernameField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        UsernameField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(63, 82, 118)));
        UsernameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UsernameFieldActionPerformed(evt);
            }
        });
        jPanel3.add(UsernameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 120, 300, 30));

        PasswordText.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        PasswordText.setBackground(new java.awt.Color(14, 22, 36));
        PasswordText.setForeground(new java.awt.Color(224, 232, 255));
        PasswordText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PasswordText.setText("PASSWORD");
        jPanel3.add(PasswordText, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 180, 420, 20));

        PasswordField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        PasswordField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(63, 82, 118)));
        PasswordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PasswordFieldActionPerformed(evt);
            }
        });
        jPanel3.add(PasswordField, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 210, 300, 30));

        LoginBtnPanel.setBackground(new java.awt.Color(0, 163, 173));
        LoginBtnPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        LoginText.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        LoginText.setForeground(new java.awt.Color(255, 255, 255));
        LoginText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LoginText.setText("LOGIN");
        LoginText.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout LoginBtnPanelLayout = new javax.swing.GroupLayout(LoginBtnPanel);
        LoginBtnPanel.setLayout(LoginBtnPanelLayout);
        LoginBtnPanelLayout.setHorizontalGroup(
            LoginBtnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(LoginText, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
        );
        LoginBtnPanelLayout.setVerticalGroup(
            LoginBtnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(LoginText, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
        );

        jPanel3.add(LoginBtnPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 260, 160, 40));

        RegisterPanel.setBackground(new java.awt.Color(18, 32, 52));

        jLabel1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 13)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 163, 173));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout RegisterPanelLayout = new javax.swing.GroupLayout(RegisterPanel);
        RegisterPanel.setLayout(RegisterPanelLayout);
        RegisterPanelLayout.setHorizontalGroup(
            RegisterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
        );
        RegisterPanelLayout.setVerticalGroup(
            RegisterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        jPanel3.add(RegisterPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 330, 420, 40));

        Welcoming.setBackground(new java.awt.Color(18, 32, 52));
        Welcoming.setFont(new java.awt.Font("Segoe UI Semibold", 0, 22)); // NOI18N
        Welcoming.setForeground(new java.awt.Color(236, 244, 255));
        Welcoming.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Welcoming.setText("Welcome to Parking Slotwise");
        jPanel3.add(Welcoming, new org.netbeans.lib.awtextra.AbsoluteConstraints(-1, 30, 420, -1));

        Extra.setBackground(new java.awt.Color(18, 32, 52));
        Extra.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        Extra.setForeground(new java.awt.Color(139, 152, 176));
        Extra.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Extra.setText("Doesn't have an account? ");
        jPanel3.add(Extra, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 310, 180, -1));

        MainFrame.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 420, 430));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainFrame, javax.swing.GroupLayout.PREFERRED_SIZE, 444, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainFrame, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        getAccessibleContext().setAccessibleParent(MainFrame);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void UsernameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_UsernameFieldActionPerformed
    }//GEN-LAST:event_UsernameFieldActionPerformed

    private void PasswordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PasswordFieldActionPerformed
    }//GEN-LAST:event_PasswordFieldActionPerformed

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Mainframe.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new Mainframe().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Extra;
    private javax.swing.JPanel LoginBtnPanel;
    private javax.swing.JLabel LoginText;
    private javax.swing.JPanel MainFrame;
    private javax.swing.JPasswordField PasswordField;
    private javax.swing.JLabel PasswordText;
    private javax.swing.JPanel RegisterPanel;
    private javax.swing.JTextField UsernameField;
    private javax.swing.JLabel UsernameText;
    private javax.swing.JLabel Welcoming;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel3;
    // End of variables declaration//GEN-END:variables
}
