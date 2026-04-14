package Main;

import Admin.AdminSettings;
import Admin.ParkedVehicleFrame;
import Admin.ParkingSlotFrame;
import Admin.ReservationFrame;
import User.ParkingSlotUserFrame;
import User.UserDash;
import Cashier.CashierDash;
import Cashier.CashierParkingFrame;
import Cashier.CompletedTransactionsFrame;
import Configuration.AppTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Cursor;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JInternalFrame;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;

public class MainPage extends javax.swing.JFrame {

    private final String role;
    private final int currentUserId;

    /** Cashier-only nav; null for admin/user. */
    private javax.swing.JPanel COMPLETEDTRANSACTIONPANEL;
    private javax.swing.JLabel COMPLETEDTRANSACTIONTEXT;

    /** Admin-only: parked / occupied bays (below Reservations). */
    private javax.swing.JPanel PARKEDVEHICLEPANEL;
    private javax.swing.JLabel PARKEDVEHICLETEXT;

    /** Main dashboard (Settings + Logout). */
    public MainPage(String role, int currentUserId) {
        this.role = role == null ? "user" : role;
        this.currentUserId = currentUserId;
        initComponents();
        setupCashierSidebarExtras();
        setupAdminParkedVehicleNav();
        applyAppTheme();
        setupPanelListeners();
        setupDesktopResizeListener();
    }

    /** Resolves the host frame for a {@link JInternalFrame} on this desktop (e.g. cashier actions). */
    public static MainPage findHostingMainPage(java.awt.Component c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        return w instanceof MainPage ? (MainPage) w : null;
    }

    /** Open completed-transaction history (cashier). */
    public void openCompletedTransactions() {
        if (!"cashier".equals(role.trim().toLowerCase())) {
            return;
        }
        openInternalFrame(new CompletedTransactionsFrame());
    }

    private void applyAppTheme() {
        MainPanel.setBackground(AppTheme.CONTENT_BG);
        DashPanel.setBackground(AppTheme.SIDEBAR_BG);
        SETTINGS.setBackground(AppTheme.NAV_BTN_BG);
        LogoutPanel.setBackground(AppTheme.NAV_BTN_BG);
        PARKINGSLOTPANEL.setBackground(AppTheme.NAV_BTN_BG);
        RESERVATIONPANEL.setBackground(AppTheme.NAV_BTN_BG);
        if (PARKEDVEHICLEPANEL != null) {
            PARKEDVEHICLEPANEL.setBackground(AppTheme.NAV_BTN_BG);
        }
        SETTINGSTEXT1.setForeground(AppTheme.TEXT_ON_NAV);
        SETTINGSTEXT1.setFont(AppTheme.FONT_NAV);
        LogoutText.setForeground(AppTheme.TEXT_ON_NAV);
        LogoutText.setFont(AppTheme.FONT_NAV);
        SLOTSTEXT1.setForeground(AppTheme.TEXT_ON_NAV);
        SLOTSTEXT1.setFont(AppTheme.FONT_NAV);
        RESERVATIONTEXT.setForeground(AppTheme.TEXT_ON_NAV);
        RESERVATIONTEXT.setFont(AppTheme.FONT_NAV);
        if (PARKEDVEHICLETEXT != null) {
            PARKEDVEHICLETEXT.setForeground(AppTheme.TEXT_ON_NAV);
            PARKEDVEHICLETEXT.setFont(AppTheme.FONT_NAV);
        }
        jLabel3.setForeground(AppTheme.TEXT_PRIMARY);
        jLabel3.setFont(AppTheme.FONT_TITLE);

        if ("cashier".equals(role.trim().toLowerCase())) {
            SLOTSTEXT1.setText("ACTIVE PARKING");
        }

        // Make window content fill when maximized or resized
        MainPanel.remove(DashPanel);
        MainPanel.remove(jDesktopPane1);
        MainPanel.setLayout(new BorderLayout());
        DashPanel.setPreferredSize(new Dimension(150, 450));
        MainPanel.add(DashPanel, BorderLayout.WEST);
        MainPanel.add(jDesktopPane1, BorderLayout.CENTER);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().remove(MainPanel);
        getContentPane().add(MainPanel, BorderLayout.CENTER);
    }

    private void setupCashierSidebarExtras() {
        if (!"cashier".equals(role.trim().toLowerCase())) {
            return;
        }
        COMPLETEDTRANSACTIONPANEL = new javax.swing.JPanel();
        COMPLETEDTRANSACTIONPANEL.setBackground(AppTheme.NAV_BTN_BG);
        COMPLETEDTRANSACTIONPANEL.setCursor(new Cursor(Cursor.HAND_CURSOR));
        COMPLETEDTRANSACTIONTEXT = new javax.swing.JLabel();
        COMPLETEDTRANSACTIONTEXT.setFont(AppTheme.FONT_NAV);
        COMPLETEDTRANSACTIONTEXT.setForeground(AppTheme.TEXT_ON_NAV);
        COMPLETEDTRANSACTIONTEXT.setHorizontalAlignment(SwingConstants.CENTER);
        COMPLETEDTRANSACTIONTEXT.setText("<html><div style='text-align:center'>COMPLETED<br>TRANSACTION</div></html>");
        javax.swing.GroupLayout gl = new javax.swing.GroupLayout(COMPLETEDTRANSACTIONPANEL);
        COMPLETEDTRANSACTIONPANEL.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(COMPLETEDTRANSACTIONTEXT, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE));
        gl.setVerticalGroup(gl.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(COMPLETEDTRANSACTIONTEXT, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE));
        DashPanel.add(COMPLETEDTRANSACTIONPANEL, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 285, 130, 48));
        COMPLETEDTRANSACTIONPANEL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                COMPLETEDTRANSACTIONPANEL.setBackground(AppTheme.NAV_BTN_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                COMPLETEDTRANSACTIONPANEL.setBackground(AppTheme.NAV_BTN_BG);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openCompletedTransactions();
            }
        });
    }

    private void setupAdminParkedVehicleNav() {
        if (!"admin".equals(role.trim().toLowerCase())) {
            return;
        }
        PARKEDVEHICLEPANEL = new javax.swing.JPanel();
        PARKEDVEHICLEPANEL.setBackground(AppTheme.NAV_BTN_BG);
        PARKEDVEHICLEPANEL.setCursor(new Cursor(Cursor.HAND_CURSOR));
        PARKEDVEHICLETEXT = new javax.swing.JLabel();
        PARKEDVEHICLETEXT.setFont(AppTheme.FONT_NAV);
        PARKEDVEHICLETEXT.setForeground(AppTheme.TEXT_ON_NAV);
        PARKEDVEHICLETEXT.setHorizontalAlignment(SwingConstants.CENTER);
        PARKEDVEHICLETEXT.setText("<html><div style='text-align:center'>PARKED<br>VEHICLE</div></html>");
        javax.swing.GroupLayout gl = new javax.swing.GroupLayout(PARKEDVEHICLEPANEL);
        PARKEDVEHICLEPANEL.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(PARKEDVEHICLETEXT, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE));
        gl.setVerticalGroup(gl.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(PARKEDVEHICLETEXT, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE));
        DashPanel.add(PARKEDVEHICLEPANEL, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 285, 130, 48));
        PARKEDVEHICLEPANEL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                PARKEDVEHICLEPANEL.setBackground(AppTheme.NAV_BTN_HOVER);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                PARKEDVEHICLEPANEL.setBackground(AppTheme.NAV_BTN_BG);
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openInternalFrame(new ParkedVehicleFrame());
            }
        });
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        SETTINGS.setCursor(handCursor);
        SETTINGS.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String r = role.trim().toLowerCase();
                if ("admin".equals(r)) {
                    openInternalFrame(new AdminSettings(currentUserId));
                } else if ("cashier".equals(r)) {
                    openInternalFrame(new CashierDash(currentUserId));
                } else {
                    openInternalFrame(new UserDash(currentUserId));
                }
            }
        });

        LogoutPanel.setCursor(handCursor);
        LogoutPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                performLogout();
            }
        });

        PARKINGSLOTPANEL.setCursor(handCursor);
        PARKINGSLOTPANEL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String r = role.trim().toLowerCase();
                if ("admin".equals(r)) {
                    openInternalFrame(new ParkingSlotFrame());
                } else if ("user".equals(r)) {
                    openInternalFrame(new ParkingSlotUserFrame(currentUserId));
                } else if ("cashier".equals(r)) {
                    openInternalFrame(new CashierParkingFrame(currentUserId));
                } else {
                    javax.swing.JOptionPane.showMessageDialog(MainPage.this, "Parking slots are for admin, user, and cashier only.", "Access", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        RESERVATIONPANEL.setCursor(handCursor);
        RESERVATIONPANEL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openInternalFrame(new ReservationFrame(currentUserId, role));
            }
        });
    }

    private void setupDesktopResizeListener() {
        jDesktopPane1.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitAllInternalFrames();
            }
        });
    }

    private void fitAllInternalFrames() {
        int w = jDesktopPane1.getWidth();
        int h = jDesktopPane1.getHeight();
        for (JInternalFrame frame : jDesktopPane1.getAllFrames()) {
            if (frame.isVisible()) {
                frame.setBounds(0, 0, w, h);
            }
        }
    }

    private void openInternalFrame(JInternalFrame frame) {
        // Close existing frames to avoid stacking multiple pages
        for (JInternalFrame f : jDesktopPane1.getAllFrames()) {
            f.setVisible(false);
            f.dispose();
            jDesktopPane1.remove(f);
        }
        jDesktopPane1.revalidate();
        jDesktopPane1.repaint();

        jDesktopPane1.add(frame);
        frame.setBounds(0, 0, jDesktopPane1.getWidth(), jDesktopPane1.getHeight());
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        } catch (java.beans.PropertyVetoException ignored) {
        }
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MainPanel = new javax.swing.JPanel();
        DashPanel = new javax.swing.JPanel();
        SETTINGS = new javax.swing.JPanel();
        SETTINGSTEXT1 = new javax.swing.JLabel();
        LogoutPanel = new javax.swing.JPanel();
        LogoutText = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        PARKINGSLOTPANEL = new javax.swing.JPanel();
        SLOTSTEXT1 = new javax.swing.JLabel();
        RESERVATIONPANEL = new javax.swing.JPanel();
        RESERVATIONTEXT = new javax.swing.JLabel();
        jDesktopPane1 = new javax.swing.JDesktopPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        MainPanel.setBackground(new java.awt.Color(255, 255, 255));
        MainPanel.setBorder(new javax.swing.border.MatteBorder(null));
        MainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        DashPanel.setBackground(new java.awt.Color(153, 153, 153));
        DashPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        SETTINGS.setBackground(new java.awt.Color(102, 102, 102));
        SETTINGS.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                SETTINGSMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                SETTINGSMouseExited(evt);
            }
        });

        SETTINGSTEXT1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        SETTINGSTEXT1.setForeground(new java.awt.Color(255, 255, 255));
        SETTINGSTEXT1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        SETTINGSTEXT1.setText("SETTINGS");

        javax.swing.GroupLayout SETTINGSLayout = new javax.swing.GroupLayout(SETTINGS);
        SETTINGS.setLayout(SETTINGSLayout);
        SETTINGSLayout.setHorizontalGroup(
            SETTINGSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(SETTINGSTEXT1, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
        );
        SETTINGSLayout.setVerticalGroup(
            SETTINGSLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, SETTINGSLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(SETTINGSTEXT1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        DashPanel.add(SETTINGS, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 130, 40));

        LogoutPanel.setBackground(new java.awt.Color(102, 102, 102));
        LogoutPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                LogoutPanelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                LogoutPanelMouseExited(evt);
            }
        });

        LogoutText.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        LogoutText.setForeground(new java.awt.Color(255, 255, 255));
        LogoutText.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        LogoutText.setText("LOGOUT");

        javax.swing.GroupLayout LogoutPanelLayout = new javax.swing.GroupLayout(LogoutPanel);
        LogoutPanel.setLayout(LogoutPanelLayout);
        LogoutPanelLayout.setHorizontalGroup(
            LogoutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(LogoutText, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
        );
        LogoutPanelLayout.setVerticalGroup(
            LogoutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LogoutPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(LogoutText, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        DashPanel.add(LogoutPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 390, 80, 40));

        jLabel3.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("Bahnschrift", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Images/Green and Gray Parking Slotwise Logo (1).png"))); // NOI18N
        DashPanel.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 140));

        PARKINGSLOTPANEL.setBackground(new java.awt.Color(102, 102, 102));
        PARKINGSLOTPANEL.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                PARKINGSLOTPANELMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                PARKINGSLOTPANELMouseExited(evt);
            }
        });

        SLOTSTEXT1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        SLOTSTEXT1.setForeground(new java.awt.Color(255, 255, 255));
        SLOTSTEXT1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        SLOTSTEXT1.setText("PARKING SLOTS");

        javax.swing.GroupLayout PARKINGSLOTPANELLayout = new javax.swing.GroupLayout(PARKINGSLOTPANEL);
        PARKINGSLOTPANEL.setLayout(PARKINGSLOTPANELLayout);
        PARKINGSLOTPANELLayout.setHorizontalGroup(
            PARKINGSLOTPANELLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PARKINGSLOTPANELLayout.createSequentialGroup()
                .addComponent(SLOTSTEXT1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        PARKINGSLOTPANELLayout.setVerticalGroup(
            PARKINGSLOTPANELLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PARKINGSLOTPANELLayout.createSequentialGroup()
                .addComponent(SLOTSTEXT1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        DashPanel.add(PARKINGSLOTPANEL, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 190, 130, 40));

        RESERVATIONPANEL.setBackground(new java.awt.Color(102, 102, 102));
        RESERVATIONPANEL.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                RESERVATIONPANELMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                RESERVATIONPANELMouseExited(evt);
            }
        });

        RESERVATIONTEXT.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        RESERVATIONTEXT.setForeground(new java.awt.Color(255, 255, 255));
        RESERVATIONTEXT.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        RESERVATIONTEXT.setText("RESERVATIONS");

        javax.swing.GroupLayout RESERVATIONPANELLayout = new javax.swing.GroupLayout(RESERVATIONPANEL);
        RESERVATIONPANEL.setLayout(RESERVATIONPANELLayout);
        RESERVATIONPANELLayout.setHorizontalGroup(
            RESERVATIONPANELLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RESERVATIONPANELLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(RESERVATIONTEXT, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        RESERVATIONPANELLayout.setVerticalGroup(
            RESERVATIONPANELLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, RESERVATIONPANELLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(RESERVATIONTEXT, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        DashPanel.add(RESERVATIONPANEL, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 240, 130, 40));

        MainPanel.add(DashPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 450));

        javax.swing.GroupLayout jDesktopPane1Layout = new javax.swing.GroupLayout(jDesktopPane1);
        jDesktopPane1.setLayout(jDesktopPane1Layout);
        jDesktopPane1Layout.setHorizontalGroup(
            jDesktopPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 650, Short.MAX_VALUE)
        );
        jDesktopPane1Layout.setVerticalGroup(
            jDesktopPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
        );

        MainPanel.add(jDesktopPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 0, 650, 450));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(MainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void SETTINGSMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SETTINGSMouseEntered
        SETTINGS.setBackground(AppTheme.NAV_BTN_HOVER);
    }//GEN-LAST:event_SETTINGSMouseEntered

    private void SETTINGSMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SETTINGSMouseExited
       SETTINGS.setBackground(AppTheme.NAV_BTN_BG);
    }//GEN-LAST:event_SETTINGSMouseExited

    private void LogoutPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_LogoutPanelMouseEntered
        LogoutPanel.setBackground(AppTheme.NAV_BTN_HOVER);
    }//GEN-LAST:event_LogoutPanelMouseEntered

    private void LogoutPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_LogoutPanelMouseExited
       LogoutPanel.setBackground(AppTheme.NAV_BTN_BG);
    }//GEN-LAST:event_LogoutPanelMouseExited

    private void PARKINGSLOTPANELMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PARKINGSLOTPANELMouseEntered
        PARKINGSLOTPANEL.setBackground(AppTheme.NAV_BTN_HOVER);
    }//GEN-LAST:event_PARKINGSLOTPANELMouseEntered

    private void PARKINGSLOTPANELMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PARKINGSLOTPANELMouseExited
        PARKINGSLOTPANEL.setBackground(AppTheme.NAV_BTN_BG);
    }//GEN-LAST:event_PARKINGSLOTPANELMouseExited

    private void RESERVATIONPANELMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_RESERVATIONPANELMouseEntered
        RESERVATIONPANEL.setBackground(AppTheme.NAV_BTN_HOVER);
    }//GEN-LAST:event_RESERVATIONPANELMouseEntered

    private void RESERVATIONPANELMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_RESERVATIONPANELMouseExited
        RESERVATIONPANEL.setBackground(AppTheme.NAV_BTN_BG);
    }//GEN-LAST:event_RESERVATIONPANELMouseExited

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new MainPage("admin", 1).setVisible(true));
    }

    private void performLogout() {
        dispose();
        Mainframe mainframe = new Mainframe();
        mainframe.setLocationRelativeTo(null);
        mainframe.setVisible(true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel DashPanel;
    private javax.swing.JPanel LogoutPanel;
    private javax.swing.JLabel LogoutText;
    private javax.swing.JPanel MainPanel;
    private javax.swing.JPanel PARKINGSLOTPANEL;
    private javax.swing.JPanel RESERVATIONPANEL;
    private javax.swing.JLabel RESERVATIONTEXT;
    private javax.swing.JPanel SETTINGS;
    private javax.swing.JLabel SETTINGSTEXT1;
    private javax.swing.JLabel SLOTSTEXT1;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
}
