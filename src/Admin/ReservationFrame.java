package Admin;

import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.PlateUtil;
import Configuration.ReservationUtil;
import Configuration.SlotTransactionUtil;
import Configuration.StyledDialog;
import Configuration.VehiclePlateUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Reservation management: View Reservations, Reserve Slot, Delete/Cancel Reservation.
 * Opened when user clicks RESERVATIONPANEL in MainPage.
 * Admin: view all, cancel any. User: view own, reserve, cancel own.
 */
public class ReservationFrame extends InternalPageFrame {

    private final DefaultTableModel tableModel;
    private final int currentUserId;
    private final String role;
    private final boolean isAdmin;
    private final boolean isCashier;

    private javax.swing.JPanel ViewReservationsPanel;
    private javax.swing.JPanel ReserveSlotPanel;
    private javax.swing.JPanel CompleteReservationPanel;
    private javax.swing.JPanel DeleteReservationPanel;
    private javax.swing.JPanel CancelReservationPanel;
    private javax.swing.JTable jTableBookings;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private boolean reservationTableViewportListenerInstalled;

    public ReservationFrame(int currentUserId, String role) {
        super();
        this.currentUserId = currentUserId;
        this.role = role == null ? "user" : role.trim().toLowerCase();
        this.isAdmin = "admin".equals(this.role);
        this.isCashier = "cashier".equals(this.role);
        initComponents();
        tableModel = (DefaultTableModel) jTableBookings.getModel();
        tableModel.setColumnIdentifiers(new String[]{"Reservation ID", "User", "Plate", "Slot", "Reservation Time", "Valid Until", "Status", "Slot ID", "User ID"});
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTableBookings);
        makeContentFillFrame();
        setupPanelListeners();
        expireReservationsOnLoad();
        configureReservationTableColumns();
        loadReservations();
    }

    /** Date columns share extra width (capped) so Status stays next to Valid Until, not pushed to the far edge. */
    private void configureReservationTableColumns() {
        jTableBookings.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel cm = jTableBookings.getColumnModel();
        if (cm.getColumnCount() < 9) {
            return;
        }
        cm.getColumn(0).setPreferredWidth(125);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(100);
        cm.getColumn(3).setPreferredWidth(80);
        cm.getColumn(4).setPreferredWidth(215);
        cm.getColumn(5).setPreferredWidth(215);
        TableColumn statusCol = cm.getColumn(6);
        statusCol.setPreferredWidth(100);
        statusCol.setMinWidth(80);
        // No maxWidth — Status absorbs spare viewport width so no navy strip appears to the right.
        hideReservationIdColumns(cm);
        if (!reservationTableViewportListenerInstalled) {
            reservationTableViewportListenerInstalled = true;
            jScrollPane1.getViewport().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    stretchReservationColumnsFitViewport();
                }
            });
        }
    }

    private static void hideReservationIdColumns(TableColumnModel cm) {
        if (cm.getColumnCount() < 9) {
            return;
        }
        for (int i : new int[]{7, 8}) {
            TableColumn c = cm.getColumn(i);
            c.setMinWidth(0);
            c.setMaxWidth(0);
            c.setPreferredWidth(0);
            c.setWidth(0);
            c.setResizable(false);
        }
    }

    /**
     * Split spare horizontal space across Reservation Time and Valid Until (each capped), then leading
     * columns — never assign all slack to one date column (that shoved Status to the window edge).
     */
    private void stretchReservationColumnsFitViewport() {
        if (jScrollPane1 == null || jTableBookings.getColumnModel().getColumnCount() < 9) {
            return;
        }
        int vw = jScrollPane1.getViewport().getWidth();
        if (vw <= 2) {
            return;
        }
        TableColumnModel cm = jTableBookings.getColumnModel();
        int total = cm.getTotalColumnWidth();
        int extra = vw - total;
        if (extra <= 2) {
            return;
        }
        final int maxDateCol = 280;
        TableColumn c4 = cm.getColumn(4);
        TableColumn c5 = cm.getColumn(5);
        int base4 = Math.max(c4.getWidth(), c4.getPreferredWidth());
        int base5 = Math.max(c5.getWidth(), c5.getPreferredWidth());
        int half = extra / 2;
        c4.setPreferredWidth(Math.min(maxDateCol, base4 + half + (extra % 2)));
        c5.setPreferredWidth(Math.min(maxDateCol, base5 + half));

        total = cm.getTotalColumnWidth();
        extra = vw - total;
        if (extra <= 2) {
            return;
        }
        int[] leadIdx = {0, 1, 2, 3};
        int[] leadCap = {180, 165, 165, 130};
        int n = leadIdx.length;
        int per = extra / n;
        int rem = extra % n;
        for (int i = 0; i < n; i++) {
            TableColumn cx = cm.getColumn(leadIdx[i]);
            int bx = Math.max(cx.getWidth(), cx.getPreferredWidth());
            int add = per + (i < rem ? 1 : 0);
            cx.setPreferredWidth(Math.min(leadCap[i], bx + add));
        }

        // Any width still unused (all other columns at caps) goes to Status — removes empty navy area right of table.
        total = cm.getTotalColumnWidth();
        extra = vw - total;
        if (extra > 2) {
            TableColumn st = cm.getColumn(6);
            int base = Math.max(st.getWidth(), st.getPreferredWidth());
            st.setPreferredWidth(base + extra);
        }
    }

    private void makeContentFillFrame() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(0, 8));
        javax.swing.JPanel topPanel = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topPanel.setBackground(AppTheme.CONTENT_BG);
        topPanel.add(ViewReservationsPanel);

        // Admin can access all reservation features as well.
        // Reserve Slot / Cancel Reservation are available to admin, cashier, and user.
        topPanel.add(ReserveSlotPanel);
        if (isAdmin || isCashier) {
            topPanel.add(CompleteReservationPanel);
        }
        topPanel.add(CancelReservationPanel);
        if (isAdmin) {
            // Delete Reservation button only for admin (not cashier/user)
            topPanel.add(DeleteReservationPanel);
        }
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(jScrollPane1, BorderLayout.CENTER);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Another account has this plate in reservations (any status), or vehicles lists a different owner.
     */
    private static boolean isPlateClaimedByAnotherCustomer(Connection conn, String normalizedPlate, int reserveUserId) throws SQLException {
        if (normalizedPlate == null || normalizedPlate.trim().isEmpty() || reserveUserId <= 0) {
            return false;
        }
        final String p = normalizedPlate.trim().toUpperCase();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM reservations WHERE upper(trim(COALESCE(plate_number,''))) = ? AND user_id <> ? LIMIT 1")) {
            ps.setString(1, p);
            ps.setInt(2, reserveUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        }
        String accName = "";
        String accUser = "";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(TRIM(name), ''), COALESCE(TRIM(username), '') FROM users WHERE user_id = ?")) {
            ps.setInt(1, reserveUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    accName = nullToEmpty(rs.getString(1));
                    accUser = nullToEmpty(rs.getString(2));
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT owner_name FROM vehicles WHERE upper(trim(plate_number)) = ? ORDER BY vehicle_id LIMIT 1")) {
            ps.setString(1, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String vOwner = nullToEmpty(rs.getString("owner_name"));
                if (vOwner.isEmpty()) {
                    return false;
                }
                boolean sameAccount = (!accName.isEmpty() && accName.equalsIgnoreCase(vOwner.trim()))
                        || (!accUser.isEmpty() && accUser.equalsIgnoreCase(vOwner.trim()));
                return !sameAccount;
            }
        }
    }

    private static int countOpenReservationsForPlate(Connection conn, String normalizedPlate) throws SQLException {
        if (normalizedPlate == null || normalizedPlate.trim().isEmpty()) {
            return 0;
        }
        String p = normalizedPlate.trim().toUpperCase();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM reservations "
                        + "WHERE upper(trim(COALESCE(plate_number,''))) = ? AND lower(trim(status)) IN ('active', 'used')")) {
            ps.setString(1, p);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Owner on {@code vehicles} may equal {@code users.name} (park flow) or {@code users.username} (reservations grid label).
     * Use the same string already on file when it clearly belongs to this account so Complete Reservation does not fail.
     */
    private static String resolveOwnerForCompleteReservation(Connection conn, int userId, String normalizedPlate,
            String reservationCustomerName) throws SQLException {
        String fromReservation = reservationCustomerName != null ? reservationCustomerName.trim() : "";
        if (!fromReservation.isEmpty()) {
            return fromReservation;
        }
        String accName = "";
        String accUsername = "";
        if (userId > 0) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT name, username FROM users WHERE user_id = ?")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        accName = nullToEmpty(rs.getString("name"));
                        accUsername = nullToEmpty(rs.getString("username"));
                    }
                }
            }
        }
        String plateUp = normalizedPlate.trim().toUpperCase();
        String existingOwner = "";
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT owner_name FROM vehicles WHERE upper(trim(plate_number)) = ? ORDER BY vehicle_id LIMIT 1")) {
            ps.setString(1, plateUp);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    existingOwner = nullToEmpty(rs.getString("owner_name"));
                }
            }
        }
        if (!existingOwner.isEmpty()) {
            boolean matchesName = !accName.isEmpty() && accName.trim().equalsIgnoreCase(existingOwner.trim());
            boolean matchesUsername = !accUsername.isEmpty() && accUsername.trim().equalsIgnoreCase(existingOwner.trim());
            if (matchesName || matchesUsername) {
                return existingOwner;
            }
        }
        if (!accName.isEmpty()) {
            return accName.trim();
        }
        if (!accUsername.isEmpty()) {
            return accUsername.trim();
        }
        return "Walk-in";
    }

    /** True when slot designation matches chosen vehicle type (Car / Motorcycle). */
    private static boolean slotTypeMatchesVehicle(String slotType, String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return false;
        }
        if (slotType == null || slotType.trim().isEmpty()) {
            return true;
        }
        return slotType.trim().equalsIgnoreCase(vehicleType.trim());
    }

    private static String fetchSlotType(Connection conn, int slotId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT slot_type FROM parking_slots WHERE slot_id = ?")) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("slot_type");
                }
            }
        }
        return null;
    }

    private static final String PLATE_CHOICE_OTHER = "— Other (type plate below) —";

    /**
     * Distinct plates for this customer (reservations for user_id, then vehicles by owner name).
     * Order: most recently seen first within each source.
     */
    private static List<String> listPlatesForUser(Connection conn, int userId, String userName) throws SQLException {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT plate_number FROM reservations "
                        + "WHERE user_id = ? AND plate_number IS NOT NULL AND trim(plate_number) != '' "
                        + "ORDER BY reservation_id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String p = PlateUtil.normalize(rs.getString(1));
                    if (!p.isEmpty()) {
                        ordered.add(p);
                    }
                }
            }
        }
        if (userName != null && !userName.trim().isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT plate_number FROM vehicles "
                            + "WHERE lower(trim(owner_name)) = lower(trim(?)) AND plate_number IS NOT NULL AND trim(plate_number) != '' "
                            + "ORDER BY vehicle_id DESC")) {
                ps.setString(1, userName.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String p = PlateUtil.normalize(rs.getString(1));
                        if (!p.isEmpty()) {
                            ordered.add(p);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(ordered);
    }

    /** Shown only under the plate combo when the user picks "Other" (type a plate). */
    private static void syncPlateStackExtras(JComboBox<String> plateCombo, JTextField plateField,
            Component plateGapStrut, JLabel typePlateLabel) {
        boolean typingRow = plateCombo.isVisible() && plateField.isVisible();
        plateGapStrut.setVisible(typingRow);
        typePlateLabel.setVisible(typingRow);
    }

    private static void applyPlatePickerState(JComboBox<String> plateCombo, JTextField plateField,
            Component plateGapStrut, JLabel typePlateLabel, Connection conn, int userId, String userName)
            throws SQLException {
        for (java.awt.event.ItemListener il : plateCombo.getItemListeners()) {
            plateCombo.removeItemListener(il);
        }
        plateCombo.removeAllItems();
        plateCombo.setVisible(false);
        plateField.setEnabled(true);
        plateField.setVisible(true);
        plateGapStrut.setVisible(false);
        typePlateLabel.setVisible(false);
        if (userId < 0) {
            plateField.setText("");
            plateField.setEditable(true);
            return;
        }
        List<String> plates = listPlatesForUser(conn, userId, userName);
        if (plates.size() >= 2) {
            for (String p : plates) {
                plateCombo.addItem(p);
            }
            plateCombo.addItem(PLATE_CHOICE_OTHER);
            plateCombo.setVisible(true);
            // Selection is only from the combo; hide the text field until "Other" (no duplicate row).
            plateField.setText("");
            plateField.setVisible(false);
            plateField.setEditable(true);
            syncPlateStackExtras(plateCombo, plateField, plateGapStrut, typePlateLabel);
            plateCombo.addItemListener(ev -> {
                if (ev.getStateChange() != ItemEvent.SELECTED) {
                    return;
                }
                Object sel = plateCombo.getSelectedItem();
                if (PLATE_CHOICE_OTHER.equals(sel)) {
                    plateField.setText("");
                    plateField.setEditable(true);
                    plateField.setVisible(true);
                    syncPlateStackExtras(plateCombo, plateField, plateGapStrut, typePlateLabel);
                    SwingUtilities.invokeLater(() -> {
                        plateField.requestFocusInWindow();
                        javax.swing.JRootPane rp = plateField.getRootPane();
                        if (rp != null) {
                            rp.revalidate();
                            rp.repaint();
                        }
                        Window w = SwingUtilities.getWindowAncestor(plateField);
                        if (w != null) {
                            w.pack();
                        }
                    });
                } else if (sel != null) {
                    plateField.setText("");
                    plateField.setVisible(false);
                    syncPlateStackExtras(plateCombo, plateField, plateGapStrut, typePlateLabel);
                    Window w = SwingUtilities.getWindowAncestor(plateField);
                    if (w != null) {
                        w.pack();
                    }
                }
            });
            plateCombo.setSelectedIndex(0);
        } else if (plates.size() == 1) {
            plateField.setText(plates.get(0));
            plateField.setEditable(true);
            plateField.setVisible(true);
            syncPlateStackExtras(plateCombo, plateField, plateGapStrut, typePlateLabel);
        } else {
            plateField.setText("");
            plateField.setEditable(true);
            plateField.setVisible(true);
            syncPlateStackExtras(plateCombo, plateField, plateGapStrut, typePlateLabel);
        }
    }

    private void expireReservationsOnLoad() {
        try (Connection conn = ConnectionConfig.getConnection()) {
            ReservationUtil.expireOldReservations(conn);
        } catch (Exception ignored) { }
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        ViewReservationsPanel.setCursor(handCursor);
        ViewReservationsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { loadReservations(); }
        });

        ReserveSlotPanel.setCursor(handCursor);
        ReserveSlotPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performReserveSlot(); }
        });

        // Cancel is available for all roles (users can cancel their own reservations),
        // Delete is only for admin/cashier.
        CancelReservationPanel.setCursor(handCursor);
        CancelReservationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performCancelReservation(); }
        });
        // Only admin can permanently delete reservations
        if (isAdmin) {
            DeleteReservationPanel.setCursor(handCursor);
            DeleteReservationPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { performDeleteReservation(); }
            });
        }
        if (isAdmin || isCashier) {
            CompleteReservationPanel.setCursor(handCursor);
            CompleteReservationPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) { performCompleteReservation(); }
            });
        }
    }

    private void performReserveSlot() {
        int reserveUserId;
        String customerName = null;
        String plateNumber = "";
        if (isAdmin || isCashier) {
            Connection conn = null;
            try {
                conn = ConnectionConfig.getConnection();
                final Connection connForPlateSuggest = conn;
                List<Object[]> users = new ArrayList<>();
                try (ResultSet rs = conn.createStatement().executeQuery(
                        "SELECT user_id, name, username FROM users "
                                + "WHERE lower(trim(username)) != 'walkin' AND lower(trim(role)) = 'user' "
                                + "AND COALESCE(lower(trim(status)), 'approved') IN ('approved', 'pending') "
                                + "ORDER BY name")) {
                    while (rs.next()) {
                        users.add(new Object[]{rs.getInt("user_id"), rs.getString("name"), rs.getString("username")});
                    }
                }
                String[] userOptions = new String[users.size() + 1];
                userOptions[0] = "-- Select customer (user account) --";
                for (int i = 0; i < users.size(); i++) {
                    Object[] u = users.get(i);
                    userOptions[i + 1] = u[1] + " (" + u[2] + ")";
                }
                if (users.isEmpty()) {
                    StyledDialog.showPlainMessage(this, "Reserve Slot",
                            "No customer accounts found. Add users with role \"user\" (not admin/cashier) first.");
                    return;
                }
                JComboBox<String> userCombo = new JComboBox<>(userOptions);
                JComboBox<String> plateCombo = new JComboBox<>();
                plateCombo.setVisible(false);
                JTextField plateField = new JTextField(25);
                plateField.setToolTipText("When you choose “Other”, type the full plate number here.");
                plateField.setForeground(AppTheme.TEXT_PRIMARY);
                plateField.setBackground(Color.WHITE);
                plateField.setCaretColor(AppTheme.TEXT_PRIMARY);
                JLabel typePlateLabel = new JLabel("Type plate number:");
                typePlateLabel.setFont(AppTheme.FONT_LABEL);
                typePlateLabel.setForeground(AppTheme.TEXT_MUTED);
                typePlateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                typePlateLabel.setVisible(false);
                Component plateGapStrut = Box.createVerticalStrut(8);
                plateGapStrut.setVisible(false);
                JPanel plateCell = new JPanel();
                plateCell.setLayout(new BoxLayout(plateCell, BoxLayout.Y_AXIS));
                plateCell.setOpaque(false);
                plateCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
                typePlateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                typePlateLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
                plateField.setAlignmentX(Component.LEFT_ALIGNMENT);
                int fieldMinH = Math.max(36, plateField.getPreferredSize().height);
                plateField.setMinimumSize(new Dimension(220, fieldMinH));
                int comboBarH = Math.max(26, plateCombo.getPreferredSize().height);
                plateCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboBarH));
                plateCell.add(plateCombo);
                plateCell.add(plateGapStrut);
                plateCell.add(typePlateLabel);
                plateCell.add(plateField);
                userCombo.addItemListener(e -> {
                    if (e.getStateChange() != ItemEvent.SELECTED) {
                        return;
                    }
                    int idx = userCombo.getSelectedIndex();
                    if (idx <= 0) {
                        try {
                            applyPlatePickerState(plateCombo, plateField, plateGapStrut, typePlateLabel,
                                    connForPlateSuggest, -1, "");
                        } catch (SQLException ex) {
                            plateField.setText("");
                        }
                        return;
                    }
                    Object[] row = users.get(idx - 1);
                    int uid = (Integer) row[0];
                    String uname = row[1] != null ? row[1].toString() : "";
                    try {
                        applyPlatePickerState(plateCombo, plateField, plateGapStrut, typePlateLabel,
                                connForPlateSuggest, uid, uname);
                    } catch (SQLException ex) {
                        plateField.setText("");
                        plateCombo.setVisible(false);
                        plateGapStrut.setVisible(false);
                        typePlateLabel.setVisible(false);
                        plateField.setVisible(true);
                    }
                });
                JPanel formGrid = new JPanel(new GridBagLayout());
                formGrid.setOpaque(false);
                formGrid.setBorder(new EmptyBorder(4, 2, 4, 2));
                GridBagConstraints fg = new GridBagConstraints();
                fg.anchor = GridBagConstraints.FIRST_LINE_START;
                fg.gridy = 0;
                fg.gridx = 0;
                fg.weightx = 0;
                fg.weighty = 0;
                fg.fill = GridBagConstraints.NONE;
                fg.insets = new Insets(2, 4, 12, 12);
                JLabel selectUserLbl = new JLabel("Select user:");
                selectUserLbl.setFont(AppTheme.FONT_LABEL);
                formGrid.add(selectUserLbl, fg);
                fg.gridx = 1;
                fg.weightx = 1;
                fg.fill = GridBagConstraints.HORIZONTAL;
                fg.insets = new Insets(2, 0, 12, 4);
                formGrid.add(userCombo, fg);
                fg.gridy = 1;
                fg.gridx = 0;
                fg.weightx = 0;
                fg.fill = GridBagConstraints.NONE;
                fg.insets = new Insets(0, 4, 0, 12);
                JLabel plateNumLbl = new JLabel("Plate number:");
                plateNumLbl.setFont(AppTheme.FONT_LABEL);
                formGrid.add(plateNumLbl, fg);
                fg.gridx = 1;
                fg.weightx = 1;
                fg.fill = GridBagConstraints.HORIZONTAL;
                fg.insets = new Insets(0, 0, 0, 4);
                formGrid.add(plateCell, fg);
                javax.swing.JLabel plateHint = new javax.swing.JLabel(
                        "<html><body style='width:300px'>If this customer has more than one plate on file, pick the correct one from the list. "
                                + "Otherwise type the plate (or choose &quot;Other&quot;).</html>");
                plateHint.setFont(AppTheme.FONT_LABEL);
                plateHint.setForeground(AppTheme.TEXT_MUTED);
                JPanel panel = new JPanel(new BorderLayout(0, 10));
                panel.setOpaque(false);
                panel.add(formGrid, BorderLayout.CENTER);
                panel.add(plateHint, BorderLayout.SOUTH);
                if (StyledDialog.showFormConfirm(this, panel, "Reserve Slot — Select customer") != JOptionPane.OK_OPTION) {
                    return;
                }
                int selIdx = userCombo.getSelectedIndex();
                if (plateCombo.isVisible() && plateCombo.getSelectedItem() != null) {
                    String sel = String.valueOf(plateCombo.getSelectedItem());
                    if (PLATE_CHOICE_OTHER.equals(sel)) {
                        plateNumber = PlateUtil.normalize(plateField.getText());
                    } else {
                        plateNumber = PlateUtil.normalize(sel);
                    }
                } else {
                    plateNumber = PlateUtil.normalize(plateField.getText());
                }
                if (plateNumber.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter or select a plate number before clicking OK.",
                            "Reserve Slot",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String errPlate = PlateUtil.validate(plateNumber);
                if (errPlate != null) {
                    JOptionPane.showMessageDialog(this, errPlate, "Reserve Slot", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Same account may reuse a plate on a new reservation after prior ones finish; open Active/Used blocks everyone.
                if (countOpenReservationsForPlate(conn, plateNumber) > 0) {
                    JOptionPane.showMessageDialog(this,
                            "This plate already has an open reservation (not yet finished). "
                                    + "Complete or cancel it before reserving this plate again.",
                            "Reserve Slot",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (selIdx > 0) {
                    reserveUserId = (Integer) users.get(selIdx - 1)[0];
                    customerName = (String) users.get(selIdx - 1)[1];
                } else {
                    JOptionPane.showMessageDialog(this, "Select a customer from the list.", "Reserve Slot", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int existing = ReservationUtil.countActiveReservations(conn, reserveUserId);
                if (existing > 0) {
                    JOptionPane.showMessageDialog(this, "This user already has an active reservation. Cancel it first.", "Reserve Slot", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (isPlateClaimedByAnotherCustomer(conn, plateNumber, reserveUserId)) {
                    JOptionPane.showMessageDialog(this,
                            "This plate is already used by another customer (reservation or parking record).\n"
                                    + "Use a different plate, or remove/change the other customer's data first.",
                            "Reserve Slot",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Failed to load users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } finally {
                ConnectionConfig.close(conn);
            }
        } else {
            reserveUserId = currentUserId;
            // Prevent regular users from having more than one Active reservation
            Connection connCheck = null;
            try {
                connCheck = ConnectionConfig.getConnection();
                int existing = ReservationUtil.countActiveReservations(connCheck, reserveUserId);
                if (existing > 0) {
                    JOptionPane.showMessageDialog(this,
                            "You already have an active reservation. Please cancel it before reserving another slot.",
                            "Reserve Slot",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Could not check existing reservations: " + e.getMessage(),
                        "Reserve Slot",
                        JOptionPane.ERROR_MESSAGE);
                return;
            } finally {
                ConnectionConfig.close(connCheck);
            }
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            java.util.List<String> slotNames = new java.util.ArrayList<>();
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT ps.slot_id, ps.slot_number FROM parking_slots ps "
                            + "WHERE lower(trim(ps.status)) = 'available' AND " + SlotTransactionUtil.NO_ACTIVE_PARKING_ON_SLOT
                            + " ORDER BY ps.slot_number")) {
                while (rs.next()) {
                    slotNames.add(rs.getString("slot_number") + " (ID:" + rs.getInt("slot_id") + ")");
                }
            }
            if (slotNames.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No slots available to reserve. Ask admin to add slots.", "Reserve Slot", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JPanel slotPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            slotPanel.setOpaque(false);
            JComboBox<String> slotCombo = new JComboBox<>(slotNames.toArray(new String[0]));
            JTextField slotPlateField = new JTextField(plateNumber != null ? plateNumber : "", 20);
            slotPlateField.setToolTipText(isAdmin || isCashier ? "Admin/cashier can enter or modify plate number" : "Enter vehicle plate number");
            slotPlateField.setForeground(AppTheme.TEXT_PRIMARY);
            slotPlateField.setBackground(Color.WHITE);
            slotPlateField.setCaretColor(AppTheme.TEXT_PRIMARY);
            JComboBox<String> vehicleTypeCombo = new JComboBox<>(new String[]{"Car", "Motorcycle"});
            slotPanel.add(new javax.swing.JLabel("Select a slot to reserve:"));
            slotPanel.add(slotCombo);
            slotPanel.add(new javax.swing.JLabel("Plate number:"));
            slotPanel.add(slotPlateField);
            slotPanel.add(new javax.swing.JLabel("Vehicle type:"));
            slotPanel.add(vehicleTypeCombo);
            if (StyledDialog.showFormConfirm(this, slotPanel, "Reserve slot — Choose bay") != JOptionPane.OK_OPTION) {
                return;
            }
            String selected = (String) slotCombo.getSelectedItem();
            String vehicleTypeChosen = vehicleTypeCombo.getSelectedItem() != null
                    ? String.valueOf(vehicleTypeCombo.getSelectedItem())
                    : "";
            plateNumber = PlateUtil.normalize(slotPlateField.getText());
            if (plateNumber.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter the vehicle plate number before clicking OK.",
                        "Reserve Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String err = PlateUtil.validate(plateNumber);
            if (err != null) {
                JOptionPane.showMessageDialog(this, err, "Reserve Slot", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (countOpenReservationsForPlate(conn, plateNumber) > 0) {
                JOptionPane.showMessageDialog(this,
                        "This plate already has an open reservation (not yet finished). "
                                + "Complete or cancel it before reserving this plate again.",
                        "Reserve Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (isPlateClaimedByAnotherCustomer(conn, plateNumber, reserveUserId)) {
                JOptionPane.showMessageDialog(this,
                        "This plate is already used by another customer (reservation or parking record).\n"
                                + "Use a different plate, or remove/change the other customer's data first.",
                        "Reserve Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selected == null) return;
            int slotId = Integer.parseInt(selected.replaceAll(".*ID:(\\d+).*", "$1"));
            String slotTypeForBay = fetchSlotType(conn, slotId);
            if (!slotTypeMatchesVehicle(slotTypeForBay, vehicleTypeChosen)) {
                JOptionPane.showMessageDialog(this,
                        "This bay is for " + nullToEmpty(slotTypeForBay) + " only.\n"
                                + "You selected " + vehicleTypeChosen + ".\n\n"
                                + "Choose a matching slot or change vehicle type.",
                        "Reserve Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (SlotTransactionUtil.countActiveTransactionsOnSlot(conn, slotId) > 0) {
                JOptionPane.showMessageDialog(this,
                        "This slot is not free: a vehicle is still checked in. Release it in Cashier before reserving.",
                        "Reserve Slot",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // No longer require the same plate as the user's last reservation on this slot:
            // customers may have multiple vehicles; admin picks the correct plate from the UI.

            String reservationTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String validUntil = ReservationUtil.computeValidUntil();
            if (customerName != null && !customerName.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO reservations (user_id, slot_id, reservation_time, status, customer_name, valid_until, plate_number) VALUES (?, ?, ?, 'Active', ?, ?, ?)")) {
                    ps.setInt(1, reserveUserId);
                    ps.setInt(2, slotId);
                    ps.setString(3, reservationTime);
                    ps.setString(4, customerName);
                    ps.setString(5, validUntil);
                    ps.setString(6, plateNumber != null ? plateNumber : "");
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO reservations (user_id, slot_id, reservation_time, status, valid_until, plate_number) VALUES (?, ?, ?, 'Active', ?, ?)")) {
                    ps.setInt(1, reserveUserId);
                    ps.setInt(2, slotId);
                    ps.setString(3, reservationTime);
                    ps.setString(4, validUntil);
                    ps.setString(5, plateNumber != null ? plateNumber : "");
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement up = conn.prepareStatement("UPDATE parking_slots SET status = 'Occupied' WHERE slot_id = ?")) {
                up.setInt(1, slotId);
                up.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Slot reserved successfully. Valid for " + ReservationUtil.VALIDITY_MINUTES + " minutes. You can view it in the table below.", "Reserve Slot", JOptionPane.INFORMATION_MESSAGE);
            loadReservations();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to reserve slot: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performCompleteReservation() {
        int row = jTableBookings.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount() || tableModel.getColumnCount() < 9) {
            JOptionPane.showMessageDialog(this, "Select an Active reservation row to complete.", "Complete Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object statusObj = tableModel.getValueAt(row, 6);
        if (!"Active".equalsIgnoreCase(String.valueOf(statusObj))) {
            JOptionPane.showMessageDialog(this, "Only Active reservations can be completed. Selected: " + statusObj, "Complete Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object ridObj = tableModel.getValueAt(row, 0);
        Object sidObj = tableModel.getValueAt(row, 7);
        Object uidObj = tableModel.getValueAt(row, 8);
        String customerName = "";
        String plateFromReservation = nullToEmpty((String) tableModel.getValueAt(row, 2));
        if (!(ridObj instanceof Number) || !(sidObj instanceof Number)) return;
        int reservationId = ((Number) ridObj).intValue();
        int slotId = ((Number) sidObj).intValue();
        int userId = uidObj instanceof Number ? ((Number) uidObj).intValue() : 0;
        JTextField plateField = new JTextField(plateFromReservation, 20);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Car", "Motorcycle"});
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new javax.swing.JLabel("Plate number:"));
        panel.add(plateField);
        panel.add(new javax.swing.JLabel("Vehicle type:"));
        panel.add(typeCombo);
        if (JOptionPane.showConfirmDialog(this, panel, "Complete Reservation - Enter Vehicle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String plateNo = PlateUtil.normalize(plateField.getText());
        String vehicleType = (String) typeCombo.getSelectedItem();
        if (plateNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Plate number is required.", "Complete Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String plateErr = PlateUtil.validate(plateNo);
        if (plateErr != null) {
            JOptionPane.showMessageDialog(this, plateErr, "Complete Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement rps = conn.prepareStatement("SELECT customer_name FROM reservations WHERE reservation_id = ?")) {
                rps.setInt(1, reservationId);
                try (ResultSet rrs = rps.executeQuery()) {
                    if (rrs.next()) {
                        customerName = nullToEmpty(rrs.getString("customer_name"));
                    }
                }
            }
            String slotTypeForBay = fetchSlotType(conn, slotId);
            if (!slotTypeMatchesVehicle(slotTypeForBay, vehicleType)) {
                JOptionPane.showMessageDialog(this,
                        "This bay is for " + nullToEmpty(slotTypeForBay) + " only.\n"
                                + "You selected " + vehicleType + ".\n\n"
                                + "Change vehicle type to match this slot.",
                        "Complete Reservation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (SlotTransactionUtil.countActiveTransactionsOnSlot(conn, slotId) > 0) {
                JOptionPane.showMessageDialog(this,
                        "Cannot complete reservation: this slot still has an Active parking session. Release it in Cashier first.",
                        "Complete Reservation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (VehiclePlateUtil.countActiveParkingForPlate(conn, plateNo) > 0) {
                JOptionPane.showMessageDialog(this,
                        "This plate number is already checked in (Active). Release that vehicle in Cashier first.",
                        "Complete Reservation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String ownerName = resolveOwnerForCompleteReservation(conn, userId, plateNo, customerName);
            int vehicleId = VehiclePlateUtil.findOrCreateVehicle(conn, plateNo, vehicleType, ownerName);
            String timeIn = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO parking_transactions (vehicle_id, slot_id, time_in, status) VALUES (?, ?, ?, 'Active')")) {
                ps.setInt(1, vehicleId);
                ps.setInt(2, slotId);
                ps.setString(3, timeIn);
                ps.executeUpdate();
            }
            try (PreparedStatement up = conn.prepareStatement("UPDATE parking_slots SET status = 'Occupied' WHERE slot_id = ?")) {
                up.setInt(1, slotId);
                up.executeUpdate();
            }
            try (PreparedStatement up = conn.prepareStatement("UPDATE reservations SET status = 'Used' WHERE reservation_id = ?")) {
                up.setInt(1, reservationId);
                up.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Reservation completed. Parking transaction created with time_in recorded. Time_out will be stamped when transaction is completed (Release Slot).", "Complete Reservation", JOptionPane.INFORMATION_MESSAGE);
            loadReservations();
        } catch (VehiclePlateUtil.PlateOwnerConflictException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Complete Reservation", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to complete reservation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performCancelReservation() {
        int row = jTableBookings.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount() || tableModel.getColumnCount() < 9) {
            JOptionPane.showMessageDialog(this, "Select a reservation row to cancel.", "Cancel Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object statusObj = tableModel.getValueAt(row, 6);
        if (!"Active".equalsIgnoreCase(String.valueOf(statusObj))) {
            JOptionPane.showMessageDialog(this, "Only Active reservations can be cancelled. Selected status: " + statusObj, "Cancel Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object ridObj = tableModel.getValueAt(row, 0);
        Object sidObj = tableModel.getValueAt(row, 7);
        if (!(ridObj instanceof Number) || !(sidObj instanceof Number)) return;
        int reservationId = ((Number) ridObj).intValue();
        int slotId = ((Number) sidObj).intValue();
        if (JOptionPane.showConfirmDialog(this, "Cancel this reservation? Status will be set to Cancelled and the slot will be freed.", "Cancel Reservation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("UPDATE reservations SET status = 'Cancelled' WHERE reservation_id = ?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_slots SET status = 'Available' WHERE slot_id = ?")) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Reservation cancelled and slot freed.", "Cancel Reservation", JOptionPane.INFORMATION_MESSAGE);
            loadReservations();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to cancel reservation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performDeleteReservation() {
        int row = jTableBookings.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount() || tableModel.getColumnCount() < 9) {
            JOptionPane.showMessageDialog(this, "Select a reservation row to delete.", "Delete Reservation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Admin deletion: allow deleting even if reservation status is "Used".
        // This removes the reservation record and frees the slot; any linked parking transactions remain for history.
        Object statusObj = tableModel.getValueAt(row, 6);
        Object ridObj = tableModel.getValueAt(row, 0);
        Object sidObj = tableModel.getValueAt(row, 7);
        if (!(ridObj instanceof Number) || !(sidObj instanceof Number)) return;
        int reservationId = ((Number) ridObj).intValue();
        int slotId = ((Number) sidObj).intValue();
        if (JOptionPane.showConfirmDialog(this, "Delete this reservation permanently? The slot will be freed.", "Delete Reservation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM reservations WHERE reservation_id = ?")) {
                ps.setInt(1, reservationId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE parking_slots SET status = 'Available' WHERE slot_id = ?")) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Reservation deleted and slot freed.", "Delete Reservation", JOptionPane.INFORMATION_MESSAGE);
            loadReservations();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to delete reservation: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void loadReservations() {
        tableModel.setRowCount(0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            ReservationUtil.expireOldReservations(conn);
            String sql;
            if (isAdmin || isCashier) {
                sql = "SELECT r.reservation_id, u.username, r.plate_number, p.slot_number, r.reservation_time, r.valid_until, r.status, r.slot_id, r.user_id " +
                        "FROM reservations r JOIN users u ON r.user_id = u.user_id JOIN parking_slots p ON r.slot_id = p.slot_id ORDER BY r.reservation_id ASC";
            } else {
                sql = "SELECT r.reservation_id, u.username, r.plate_number, p.slot_number, r.reservation_time, r.valid_until, r.status, r.slot_id, r.user_id " +
                        "FROM reservations r JOIN users u ON r.user_id = u.user_id JOIN parking_slots p ON r.slot_id = p.slot_id " +
                        "WHERE r.user_id = ? ORDER BY r.reservation_id ASC";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (!isAdmin && !isCashier) ps.setInt(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tableModel.addRow(new Object[]{
                                rs.getInt("reservation_id"),
                                nullToEmpty(rs.getString("username")),
                                nullToEmpty(rs.getString("plate_number")),
                                nullToEmpty(rs.getString("slot_number")),
                                nullToEmpty(rs.getString("reservation_time")),
                                nullToEmpty(rs.getString("valid_until")),
                                nullToEmpty(rs.getString("status")),
                                rs.getInt("slot_id"),
                                rs.getInt("user_id")
                        });
                    }
                }
            }
            hideReservationIdColumns(jTableBookings.getColumnModel());
            SwingUtilities.invokeLater(this::stretchReservationColumnsFitViewport);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load reservations: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            new String[]{"Reservation ID", "User", "Plate", "Slot", "Reservation Time", "Valid Until", "Status", "Slot ID", "User ID"}
        ));
        jTableBookings.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(jTableBookings);
        mainPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 80, 530, 360));

        ViewReservationsPanel = AppTheme.createNavPanel("View Reservations");
        ReserveSlotPanel = AppTheme.createNavPanel("Reserve Slot");
        CompleteReservationPanel = AppTheme.createNavPanel("Complete Reservation");
        DeleteReservationPanel = AppTheme.createNavPanel("Delete Reservation");
        CancelReservationPanel = AppTheme.createNavPanel("Cancel Reservation");

        add(mainPanel);
    }
}
