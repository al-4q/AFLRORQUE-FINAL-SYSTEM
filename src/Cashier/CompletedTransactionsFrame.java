package Cashier;

import Admin.InternalPageFrame;
import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * Cashier view of paid / finished parking sessions only. Opened from the sidebar
 * or automatically after generating a receipt from Active Parking.
 */
public class CompletedTransactionsFrame extends InternalPageFrame {

    private final DefaultTableModel tableModel;
    private javax.swing.JPanel GenerateReceiptPanel;
    private javax.swing.JPanel DeletePanel;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane jScrollPane1;

    public CompletedTransactionsFrame() {
        super();
        initComponents();
        tableModel = (DefaultTableModel) jTable1.getModel();
        tableModel.setColumnIdentifiers(new String[]{
                "Transaction ID", "Plate", "Vehicle Type", "Slot ID", "Time In", "Time Out", "Parking Fee", "Status"
        });
        mainPanel.setBackground(AppTheme.CONTENT_BG);
        AppTheme.styleTable(jTable1);
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        javax.swing.table.TableColumnModel cm = jTable1.getColumnModel();
        cm.getColumn(0).setPreferredWidth(105);
        cm.getColumn(1).setPreferredWidth(95);
        cm.getColumn(2).setPreferredWidth(95);
        cm.getColumn(3).setPreferredWidth(60);
        cm.getColumn(4).setPreferredWidth(190);
        cm.getColumn(5).setPreferredWidth(190);
        cm.getColumn(6).setPreferredWidth(85);
        cm.getColumn(7).setPreferredWidth(90);
        makeContentFillFrame();
        jScrollPane1.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                stretchLastColumnToFillViewport();
            }
        });
        setupListeners();
        loadCompletedOnly();
        SwingUtilities.invokeLater(this::stretchLastColumnToFillViewport);
    }

    /**
     * With {@code AUTO_RESIZE_OFF}, column widths can be narrower than the scroll pane; the header
     * then shows theme background (navy) in the empty strip right of "Status". Absorb that space
     * into the last column instead.
     */
    private void stretchLastColumnToFillViewport() {
        if (jScrollPane1 == null || jTable1 == null) {
            return;
        }
        int vw = jScrollPane1.getViewport().getWidth();
        if (vw <= 2) {
            return;
        }
        int total = jTable1.getColumnModel().getTotalColumnWidth();
        int extra = vw - total;
        if (extra <= 2) {
            return;
        }
        TableColumn last = jTable1.getColumnModel().getColumn(jTable1.getColumnCount() - 1);
        int base = Math.max(last.getWidth(), last.getPreferredWidth());
        last.setPreferredWidth(base + extra);
    }

    private void makeContentFillFrame() {
        mainPanel.removeAll();
        mainPanel.setLayout(new BorderLayout(0, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.setBackground(AppTheme.CONTENT_BG);
        top.add(GenerateReceiptPanel);
        top.add(DeletePanel);
        mainPanel.add(top, BorderLayout.NORTH);
        mainPanel.add(jScrollPane1, BorderLayout.CENTER);
    }

    private void setupListeners() {
        AppTheme.wireNavPanelClick(GenerateReceiptPanel, () ->
                ReceiptDialogHelper.generateReceiptForRow(CompletedTransactionsFrame.this, tableModel,
                        jTable1.getSelectedRow(), CompletedTransactionsFrame.this::loadCompletedOnly));
        AppTheme.wireNavPanelClick(DeletePanel, this::performDeleteSelected);
    }

    private void performDeleteSelected() {
        int row = jTable1.getSelectedRow();
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(this, "Select a completed transaction row first.", "Delete Transaction", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object txIdObj = tableModel.getValueAt(row, 0);
        if (!(txIdObj instanceof Number)) {
            return;
        }
        int transactionId = ((Number) txIdObj).intValue();
        if (JOptionPane.showConfirmDialog(this, "Permanently delete this completed transaction? This will also remove related payments.",
                "Delete Transaction", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM payments WHERE transaction_id = ?")) {
                ps.setInt(1, transactionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM parking_transactions WHERE transaction_id = ?")) {
                ps.setInt(1, transactionId);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Completed transaction deleted.", "Delete Transaction", JOptionPane.INFORMATION_MESSAGE);
            loadCompletedOnly();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to delete transaction: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Visible to {@link Main.MainPage} when switching views after receipt. */
    public void refreshTable() {
        loadCompletedOnly();
    }

    private void loadCompletedOnly() {
        tableModel.setRowCount(0);
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            String sql = "SELECT pt.transaction_id, v.plate_number, v.vehicle_type, pt.slot_id, pt.time_in, pt.time_out, pt.parking_fee, pt.status "
                    + "FROM parking_transactions pt JOIN vehicles v ON pt.vehicle_id = v.vehicle_id "
                    + "WHERE lower(trim(pt.status)) = 'completed' "
                    + "ORDER BY pt.transaction_id ASC";
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
            JOptionPane.showMessageDialog(this, "Failed to load completed transactions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
        SwingUtilities.invokeLater(this::stretchLastColumnToFillViewport);
    }

    private void initComponents() {
        mainPanel = new JPanel();
        jTable1 = new javax.swing.JTable();
        jTable1.setModel(new DefaultTableModel(new Object[][]{}, new String[]{}));
        jTable1.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1 = new JScrollPane(jTable1);
        setPreferredSize(new java.awt.Dimension(790, 415));
        mainPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        GenerateReceiptPanel = AppTheme.createNavPanel("Generate Receipt");
        DeletePanel = AppTheme.createNavPanel("Delete");
        add(mainPanel);
    }
}
