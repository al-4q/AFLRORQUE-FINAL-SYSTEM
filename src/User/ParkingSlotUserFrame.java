package User;

import Admin.InternalPageFrame;
import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.PlateUtil;
import Configuration.ReservationUtil;
import Configuration.SlotTransactionUtil;
import Configuration.VehiclePlateUtil;
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
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * User parking features: Park Vehicle, Parking Time, View Slots.
 * Opened when user clicks PARKING SLOTS in MainPage.
 */
public class ParkingSlotUserFrame extends InternalPageFrame {

    private final int currentUserId;
    private String currentUserName = "";
    private final DefaultTableModel tableModel;

    private javax.swing.JPanel ParkVehiclePanel;
    private javax.swing.JPanel ViewParkingTimePanel;
    private javax.swing.JPanel ViewSlotsPanel;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane jScrollPane1;

    public ParkingSlotUserFrame(int currentUserId) {
        super();
        this.currentUserId = currentUserId;
        initComponents();
        tableModel = (DefaultTableModel) jTable1.getModel();
        tableModel.setColumnIdentifiers(new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"});
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTable1);
        makeContentFillFrame();
        loadCurrentUserName();
        setupPanelListeners();
        performViewAvailableSlots();
    }

    private void makeContentFillFrame() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(0, 8));
        javax.swing.JPanel topPanel = new javax.swing.JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topPanel.setBackground(AppTheme.CONTENT_BG);
        topPanel.add(ParkVehiclePanel);
        topPanel.add(ViewParkingTimePanel);
        topPanel.add(ViewSlotsPanel);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(jScrollPane1, BorderLayout.CENTER);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void loadCurrentUserName() {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM users WHERE user_id = ?")) {
                ps.setInt(1, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentUserName = nullToEmpty(rs.getString("name"));
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load user: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void setupPanelListeners() {
        Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);

        ParkVehiclePanel.setCursor(handCursor);
        ParkVehiclePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performParkVehicle(); }
        });
        ViewParkingTimePanel.setCursor(handCursor);
        ViewParkingTimePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performViewParkingTime(); }
        });
        ViewSlotsPanel.setCursor(handCursor);
        ViewSlotsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { performViewAvailableSlots(); }
        });
    }

    private void performViewAvailableSlots() {
        tableModel.setRowCount(0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT ps.slot_id, ps.slot_number, ps.slot_type, ps.status FROM parking_slots ps "
                            + "WHERE lower(trim(ps.status)) = 'available' AND " + SlotTransactionUtil.NO_ACTIVE_PARKING_ON_SLOT
                            + " ORDER BY ps.slot_number")) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("slot_id"),
                            nullToEmpty(rs.getString("slot_number")),
                            nullToEmpty(rs.getString("slot_type")),
                            nullToEmpty(rs.getString("status"))
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load slots: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performViewParkingTime() {
        if (currentUserName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "User profile not loaded.", "View Parking Time", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT pt.time_in, pt.time_out, pt.status FROM parking_transactions pt " +
                    "JOIN vehicles v ON pt.vehicle_id = v.vehicle_id WHERE v.owner_name = ? ORDER BY pt.transaction_id DESC LIMIT 1")) {
                ps.setString(1, currentUserName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String msg = "Time in: " + nullToEmpty(rs.getString("time_in")) + "\nTime out: " + nullToEmpty(rs.getString("time_out")) + "\nStatus: " + nullToEmpty(rs.getString("status"));
                        JOptionPane.showMessageDialog(this, msg, "View Parking Time", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "No parking record found.", "View Parking Time", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to load parking time: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private void performParkVehicle() {
        if (currentUserName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "User profile not loaded. Please try again.", "Park Vehicle", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            ReservationUtil.expireOldReservations(conn);
            // New: prevent user from parking twice at the same time
            try (PreparedStatement activeTxCheck = conn.prepareStatement(
                    "SELECT COUNT(*) FROM parking_transactions pt " +
                    "JOIN vehicles v ON pt.vehicle_id = v.vehicle_id " +
                    "WHERE v.owner_name = ? AND pt.status = 'Active'")) {
                activeTxCheck.setString(1, currentUserName);
                try (ResultSet rs = activeTxCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(this,
                                "You already have an active parked vehicle. Please complete that parking first.",
                                "Park Vehicle",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
            Integer slotId = null;
            int reservedSlot = ReservationUtil.getActiveReservationSlotId(conn, currentUserId);
            int reservationId = ReservationUtil.getActiveReservationId(conn, currentUserId);
            if (reservedSlot > 0) {
                if (SlotTransactionUtil.countActiveTransactionsOnSlot(conn, reservedSlot) > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Your reserved slot still has an active parking session. Ask staff to release it first.",
                            "Park Vehicle",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                slotId = reservedSlot;
            }
            JTextField plateField = new JTextField(20);
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Car", "Motorcycle"});
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new javax.swing.JLabel("Plate number:"));
            panel.add(plateField);
            panel.add(new javax.swing.JLabel("Vehicle type:"));
            panel.add(typeCombo);
            if (JOptionPane.showConfirmDialog(this, panel, "Park Vehicle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            String plateNo = PlateUtil.normalize(plateField.getText());
            String vehicleType = (String) typeCombo.getSelectedItem();
            if (plateNo.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Plate number is required.", "Park Vehicle", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String plateErr = PlateUtil.validate(plateNo);
            if (plateErr != null) {
                JOptionPane.showMessageDialog(this, plateErr, "Park Vehicle", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (slotId != null) {
                try (PreparedStatement stPs = conn.prepareStatement(
                        "SELECT slot_type FROM parking_slots WHERE slot_id = ?")) {
                    stPs.setInt(1, slotId);
                    try (ResultSet rs = stPs.executeQuery()) {
                        if (!rs.next()) {
                            JOptionPane.showMessageDialog(this, "Reserved slot not found.", "Park Vehicle", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        String reservedSlotType = rs.getString("slot_type");
                        if (reservedSlotType == null
                                || !reservedSlotType.trim().equalsIgnoreCase(vehicleType.trim())) {
                            String need = reservedSlotType != null && !reservedSlotType.trim().isEmpty()
                                    ? reservedSlotType.trim() : "matching";
                            JOptionPane.showMessageDialog(this,
                                    "Your reserved slot is for " + need + " parking. Set Vehicle type to \"" + need + "\".",
                                    "Park Vehicle",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    }
                }
            }
            if (slotId == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT ps.slot_id FROM parking_slots ps "
                                + "WHERE lower(trim(ps.status)) = 'available' "
                                + "AND lower(trim(ps.slot_type)) = lower(trim(?)) AND "
                                + SlotTransactionUtil.NO_ACTIVE_PARKING_ON_SLOT
                                + " ORDER BY ps.slot_number LIMIT 1")) {
                    ps.setString(1, vehicleType);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            slotId = rs.getInt("slot_id");
                        }
                    }
                }
            }
            if (slotId == null) {
                JOptionPane.showMessageDialog(this,
                        "No available " + vehicleType + " slots. Ask admin to add slots, or reserve the correct bay under RESERVATIONS.",
                        "Park Vehicle",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (SlotTransactionUtil.countActiveTransactionsOnSlot(conn, slotId) > 0) {
                JOptionPane.showMessageDialog(this,
                        "This slot is no longer free (another vehicle is still checked in). Try again or pick another slot.",
                        "Park Vehicle",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (VehiclePlateUtil.countActiveParkingForPlate(conn, plateNo) > 0) {
                JOptionPane.showMessageDialog(this,
                        "This plate number is already checked in (Active). Complete or release that session in Cashier first.",
                        "Park Vehicle",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int vehicleId = VehiclePlateUtil.findOrCreateVehicle(conn, plateNo, vehicleType, currentUserName);
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
            if (reservationId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE reservations SET status = 'Used' WHERE reservation_id = ?")) {
                    ps.setInt(1, reservationId);
                    ps.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(this, "Vehicle parked successfully." + (reservationId > 0 ? " (Reservation used.)" : ""), "Park Vehicle", JOptionPane.INFORMATION_MESSAGE);
            performViewAvailableSlots();
        } catch (VehiclePlateUtil.PlateOwnerConflictException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Park Vehicle", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to park vehicle: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            new String[]{"Slot ID", "Slot Number", "Slot Type", "Status"}
        ));
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(jTable1);
        ParkVehiclePanel = AppTheme.createNavPanel("Park Vehicle");
        ViewParkingTimePanel = AppTheme.createNavPanel("Parking Time");
        ViewSlotsPanel = AppTheme.createNavPanel("View Slots");

        add(mainPanel);
    }
}
