package Cashier;

import Configuration.AppTheme;
import Configuration.ConnectionConfig;
import Configuration.ReceiptCode39;
import Configuration.StyledDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Shared receipt preview / print for completed parking transactions (Completed Transactions screen).
 */
public final class ReceiptDialogHelper {

    private ReceiptDialogHelper() { }

    private static final DateTimeFormatter ISO_LENIENT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_TS = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a", Locale.US);
    private static final DecimalFormat MONEY = new DecimalFormat("₱#,##0.00");

    private static Window dialogOwner(Component parent) {
        if (parent == null) {
            return null;
        }
        Window w = SwingUtilities.getWindowAncestor(parent);
        if (w != null) {
            return w;
        }
        return JOptionPane.getFrameForComponent(parent);
    }

    private static String formatReceiptTimestamp(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "—";
        }
        String t = raw.trim();
        try {
            return LocalDateTime.parse(t, ISO_LENIENT).format(DISPLAY_TS);
        } catch (DateTimeParseException e) {
            return t;
        }
    }

    private static String formatMoney(double amount) {
        return MONEY.format(amount);
    }

    private static String displayCell(String columnName, Object value) {
        String s = value == null ? "" : String.valueOf(value);
        if (columnName == null) {
            return s;
        }
        String lower = columnName.toLowerCase(Locale.ROOT);
        if ((lower.contains("time") || lower.contains("date")) && !s.isEmpty()) {
            return formatReceiptTimestamp(s);
        }
        if (lower.contains("fee") || lower.contains("parking fee")) {
            try {
                double d = value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(s.trim());
                return formatMoney(d);
            } catch (NumberFormatException ignored) {
                return s;
            }
        }
        return s;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Plain multi-line text for thermal / standard printing. */
    private static String buildPrintableReceipt(DefaultTableModel tableModel, int row, double amountPaid, boolean alreadyPrinted,
            String customerDisplayName, String barcodePayload, String paymentMethod,
            Double cashTendered, Double changeAmount) {
        StringBuilder sb = new StringBuilder();
        if (alreadyPrinted) {
            sb.append("(Copy — receipt was already printed.)\n\n");
        }
        sb.append("---------- PARKING RECEIPT ----------\n");
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            String name = tableModel.getColumnName(c);
            Object val = tableModel.getValueAt(row, c);
            sb.append(name).append(": ").append(displayCell(name, val)).append("\n");
            if (c == 0) {
                sb.append("Customer: ").append(customerDisplayName.isEmpty() ? "—" : customerDisplayName).append("\n");
            }
        }
        sb.append("Amount paid: ").append(formatMoney(amountPaid)).append("\n");
        if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
            sb.append("Payment method: ").append(paymentMethod.trim()).append("\n");
        }
        if (cashTendered != null && changeAmount != null) {
            sb.append("Cash received: ").append(formatMoney(cashTendered)).append("\n");
            sb.append("Change: ").append(formatMoney(changeAmount)).append("\n");
        }
        sb.append("Barcode: ").append(barcodePayload.toUpperCase(Locale.US)).append("\n");
        sb.append("--------------------------------------\nThank you for parking with us.");
        return sb.toString();
    }

    private static JPanel buildReceiptPreviewPanel(DefaultTableModel tableModel, int row, double amountPaid, boolean alreadyPrinted,
            String customerDisplayName, String barcodePayload, String paymentMethod) {
        return buildReceiptPreviewPanel(tableModel, row, amountPaid, alreadyPrinted, customerDisplayName, barcodePayload,
                paymentMethod, null, null, 48, 2);
    }

    private static JPanel buildReceiptPreviewPanel(DefaultTableModel tableModel, int row, double amountPaid, boolean alreadyPrinted,
            String customerDisplayName, String barcodePayload, String paymentMethod,
            Double cashTendered, Double changeAmount, int barcodeBarHeightPx, int barcodeModulePx) {
        JPanel outer = new JPanel(new BorderLayout(0, 0));
        outer.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.NAV_BTN_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, AppTheme.ACCENT),
                new EmptyBorder(16, 20, 16, 20)));
        JPanel headText = new JPanel();
        headText.setLayout(new BoxLayout(headText, BoxLayout.Y_AXIS));
        headText.setOpaque(false);
        JLabel title = new JLabel("Parking receipt", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Parking Slotwise", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(0xa8, 0xc4, 0xe8));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        headText.add(title);
        headText.add(Box.createVerticalStrut(4));
        headText.add(sub);
        header.add(headText, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);

        if (alreadyPrinted) {
            JLabel banner = new JLabel("  This receipt was already printed — preview copy.  ");
            banner.setFont(AppTheme.FONT_LABEL);
            banner.setForeground(new Color(0x7d, 0x5a, 0x00));
            banner.setOpaque(true);
            banner.setBackground(new Color(0xff, 0xf4, 0xcc));
            banner.setBorder(new EmptyBorder(8, 12, 8, 12));
            center.add(banner, BorderLayout.NORTH);
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(16, 22, 12, 22));
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(6, 0, 6, 16);
        int r = 0;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            String name = tableModel.getColumnName(c);
            Object val = tableModel.getValueAt(row, c);
            String text = displayCell(name, val);

            gc.gridx = 0;
            gc.gridy = r;
            gc.weightx = 0;
            JLabel lbl = new JLabel(name + ":");
            lbl.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            lbl.setForeground(new Color(0x5c, 0x6b, 0x7a));
            grid.add(lbl, gc);

            gc.gridx = 1;
            gc.weightx = 1;
            JLabel valLbl = new JLabel(text);
            valLbl.setFont(AppTheme.FONT_LABEL);
            valLbl.setForeground(AppTheme.TEXT_PRIMARY);
            grid.add(valLbl, gc);
            r++;

            if (c == 0) {
                gc.gridx = 0;
                gc.gridy = r;
                gc.weightx = 0;
                JLabel userTag = new JLabel("Customer:");
                userTag.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
                userTag.setForeground(new Color(0x5c, 0x6b, 0x7a));
                grid.add(userTag, gc);
                gc.gridx = 1;
                gc.weightx = 1;
                JLabel userVal = new JLabel(customerDisplayName.isEmpty() ? "—" : customerDisplayName);
                userVal.setFont(AppTheme.FONT_LABEL);
                userVal.setForeground(AppTheme.TEXT_PRIMARY);
                grid.add(userVal, gc);
                r++;
            }
        }

        gc.gridx = 0;
        gc.gridy = r;
        gc.gridwidth = 1;
        gc.weightx = 0;
        JLabel paidTag = new JLabel("Amount paid:");
        paidTag.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
        paidTag.setForeground(new Color(0x5c, 0x6b, 0x7a));
        grid.add(paidTag, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        JLabel paidVal = new JLabel(formatMoney(amountPaid));
        paidVal.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        paidVal.setForeground(new Color(0x0e, 0x6b, 0x5e));
        grid.add(paidVal, gc);
        r++;

        if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
            gc.gridx = 0;
            gc.gridy = r;
            gc.weightx = 0;
            JLabel pmTag = new JLabel("Payment method:");
            pmTag.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            pmTag.setForeground(new Color(0x5c, 0x6b, 0x7a));
            grid.add(pmTag, gc);
            gc.gridx = 1;
            gc.weightx = 1;
            JLabel pmVal = new JLabel(paymentMethod.trim());
            pmVal.setFont(AppTheme.FONT_LABEL);
            pmVal.setForeground(AppTheme.TEXT_PRIMARY);
            grid.add(pmVal, gc);
            r++;
        }

        if (cashTendered != null && changeAmount != null) {
            gc.gridx = 0;
            gc.gridy = r;
            gc.weightx = 0;
            JLabel ctTag = new JLabel("Cash received:");
            ctTag.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            ctTag.setForeground(new Color(0x5c, 0x6b, 0x7a));
            grid.add(ctTag, gc);
            gc.gridx = 1;
            gc.weightx = 1;
            JLabel ctVal = new JLabel(formatMoney(cashTendered));
            ctVal.setFont(AppTheme.FONT_LABEL);
            ctVal.setForeground(AppTheme.TEXT_PRIMARY);
            grid.add(ctVal, gc);
            r++;
            gc.gridx = 0;
            gc.gridy = r;
            gc.weightx = 0;
            JLabel chTag = new JLabel("Change:");
            chTag.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 12));
            chTag.setForeground(new Color(0x5c, 0x6b, 0x7a));
            grid.add(chTag, gc);
            gc.gridx = 1;
            gc.weightx = 1;
            JLabel chVal = new JLabel(formatMoney(changeAmount));
            chVal.setFont(AppTheme.FONT_LABEL);
            chVal.setForeground(AppTheme.TEXT_PRIMARY);
            grid.add(chVal, gc);
            r++;
        }

        JPanel bodyStack = new JPanel(new BorderLayout(0, 12));
        bodyStack.setOpaque(false);
        bodyStack.add(grid, BorderLayout.NORTH);

        JPanel barcodeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        barcodeRow.setOpaque(false);
        try {
            BufferedImage bc = ReceiptCode39.toBufferedImage(barcodePayload, barcodeBarHeightPx, barcodeModulePx);
            JLabel bcLbl = new JLabel(new ImageIcon(bc));
            bcLbl.setToolTipText("Code 39 — scan or use text: " + barcodePayload.toUpperCase(Locale.US));
            barcodeRow.add(bcLbl);
        } catch (RuntimeException ex) {
            JLabel fallback = new JLabel("Barcode: " + barcodePayload.toUpperCase(Locale.US));
            fallback.setFont(AppTheme.FONT_LABEL);
            barcodeRow.add(fallback);
        }
        bodyStack.add(barcodeRow, BorderLayout.CENTER);

        JLabel footer = new JLabel("Thank you for parking with us.", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        footer.setForeground(new Color(0x7f, 0x8c, 0x9a));
        footer.setBorder(new EmptyBorder(4, 16, 4, 16));
        bodyStack.add(footer, BorderLayout.SOUTH);

        center.add(bodyStack, BorderLayout.CENTER);

        outer.add(header, BorderLayout.NORTH);
        outer.add(center, BorderLayout.CENTER);
        return outer;
    }

    public static String resolveCustomerDisplayName(Connection conn, int transactionId) throws SQLException {
        String customerName = "—";
        try (PreparedStatement psCust = conn.prepareStatement(
                "SELECT COALESCE((SELECT u.username FROM users u WHERE lower(trim(COALESCE(u.name,''))) = lower(trim(COALESCE(v.owner_name,''))) LIMIT 1), "
                        + "NULLIF(trim(v.owner_name), ''), '—') AS cust "
                        + "FROM parking_transactions pt INNER JOIN vehicles v ON pt.vehicle_id = v.vehicle_id WHERE pt.transaction_id = ?")) {
            psCust.setInt(1, transactionId);
            try (ResultSet rsCust = psCust.executeQuery()) {
                if (rsCust.next()) {
                    customerName = nullToEmpty(rsCust.getString("cust"));
                }
            }
        }
        return customerName.isEmpty() ? "—" : customerName;
    }

    /**
     * True when the latest payment row for this transaction has been printed by the cashier
     * (user "PARKING RECEIPTS" must stay hidden until this is true).
     */
    public static boolean isReceiptPrintedForTransaction(Connection conn, int transactionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(receipt_printed, 0) AS rp FROM payments WHERE transaction_id = ? ORDER BY payment_id DESC LIMIT 1")) {
            ps.setInt(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getInt("rp") != 0;
            }
        }
    }

    /**
     * Read-only styled receipt with barcode (e.g. user View Receipts). Does not update payments or offer print.
     * When {@code verifyPrintedInDb} is true, refuses to open unless {@link #isReceiptPrintedForTransaction} is true.
     * Pass false only if the caller already enforced that in SQL (avoids an extra DB round-trip).
     */
    public static void showCustomerReceiptPreview(Component parent, DefaultTableModel tableModel, int row,
            double amountPaid, String customerDisplayName, int transactionId, String paymentMethod,
            boolean verifyPrintedInDb, Double cashTendered, Double changeAmount) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        if (verifyPrintedInDb) {
            Connection verifyConn = null;
            try {
                verifyConn = ConnectionConfig.getConnection();
                if (!isReceiptPrintedForTransaction(verifyConn, transactionId)) {
                    JOptionPane.showMessageDialog(parent,
                            "Your receipt is not available yet.\n\n"
                                    + "After checkout, the cashier must open Completed Transactions, then Generate Receipt "
                                    + "and print your receipt. You can view it here only after it has been printed.",
                            "Your parking receipt",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(parent, "Could not verify receipt: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } finally {
                ConnectionConfig.close(verifyConn);
            }
        }
        String cust = customerDisplayName == null || customerDisplayName.isEmpty() ? "—" : customerDisplayName;
        String barcodePayload = ReceiptCode39.parkingBarcodePayload(transactionId);
        // Slightly smaller barcode than cashier preview: faster to rasterize, still readable on screen.
        JPanel preview = buildReceiptPreviewPanel(tableModel, row, amountPaid, false, cust, barcodePayload, paymentMethod,
                cashTendered, changeAmount, 28, 2);
        showReceiptDialog(parent, preview, false);
    }

    /** Same as {@link #showCustomerReceiptPreview(Component, DefaultTableModel, int, double, String, int, String, boolean, Double, Double)} with DB verification. */
    public static void showCustomerReceiptPreview(Component parent, DefaultTableModel tableModel, int row,
            double amountPaid, String customerDisplayName, int transactionId, String paymentMethod) {
        showCustomerReceiptPreview(parent, tableModel, row, amountPaid, customerDisplayName, transactionId, paymentMethod, true, null, null);
    }

    /**
     * @return true if user chose Print (and printing may still be cancelled in the system dialog)
     */
    private static boolean showReceiptDialog(Component parent, JPanel receiptContent, boolean allowPrint) {
        final boolean[] doPrint = {false};
        Window own = dialogOwner(parent);
        JDialog dlg;
        if (own instanceof Frame) {
            dlg = new JDialog((Frame) own, "Receipt", true);
        } else if (own instanceof Dialog) {
            dlg = new JDialog((Dialog) own, "Receipt", true);
        } else {
            dlg = new JDialog((Frame) null, "Receipt", true);
        }
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(StyledDialog.wrapCard(receiptContent), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        JButton closeBtn = new JButton("Close");
        btnRow.add(closeBtn);
        if (allowPrint) {
            JButton printBtn = new JButton("Print");
            btnRow.add(printBtn);
            printBtn.addActionListener(e -> {
                doPrint[0] = true;
                dlg.dispose();
            });
        }
        root.add(btnRow, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        JRootPane rp = dlg.getRootPane();
        rp.setDefaultButton(closeBtn);
        closeBtn.addActionListener(e -> dlg.dispose());

        rp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeReceipt");
        rp.getActionMap().put("closeReceipt", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dlg.dispose();
            }
        });

        dlg.pack();
        dlg.setMinimumSize(dlg.getPreferredSize());
        dlg.setLocationRelativeTo(own);
        dlg.setVisible(true);
        return doPrint[0];
    }

    /**
     * @param afterDialogClosed optional, run on EDT after the receipt dialog is dismissed (e.g. refresh table)
     */
    public static void generateReceiptForRow(Component parent, DefaultTableModel tableModel, int row, Runnable afterDialogClosed) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            JOptionPane.showMessageDialog(parent, "Select a transaction row first.", "Generate Receipt", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object statusObj = tableModel.getValueAt(row, 7);
        if (!"Completed".equalsIgnoreCase(String.valueOf(statusObj))) {
            JOptionPane.showMessageDialog(parent,
                    "Cannot generate receipt. Transaction must be completed first. Use Release Slot on Active Parking to complete.",
                    "Generate Receipt", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object txIdObj = tableModel.getValueAt(row, 0);
        if (!(txIdObj instanceof Number)) {
            return;
        }
        int transactionId = ((Number) txIdObj).intValue();
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            Double amount = null;
            boolean alreadyPrinted = false;
            String paymentMethod = "";
            Double cashTendered = null;
            Double changeAmount = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT amount, payment_date, COALESCE(receipt_printed, 0) AS receipt_printed, "
                            + "COALESCE(NULLIF(trim(payment_method), ''), 'Cash') AS payment_method, "
                            + "cash_tendered, change_amount "
                            + "FROM payments WHERE transaction_id = ? ORDER BY payment_id DESC LIMIT 1")) {
                ps.setInt(1, transactionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        amount = rs.getDouble("amount");
                        alreadyPrinted = rs.getInt("receipt_printed") != 0;
                        paymentMethod = nullToEmpty(rs.getString("payment_method"));
                        Object ct = rs.getObject("cash_tendered");
                        if (ct != null) {
                            cashTendered = rs.getDouble("cash_tendered");
                        }
                        Object ch = rs.getObject("change_amount");
                        if (ch != null) {
                            changeAmount = rs.getDouble("change_amount");
                        }
                    }
                }
            }
            if (amount == null) {
                JOptionPane.showMessageDialog(parent,
                        "Cannot generate receipt: no payment is on file for this transaction.\n\n"
                                + "The customer must pay first (e.g. Record Cash/GCash Payment on Active Parking) before release.",
                        "Generate Receipt", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String customerName = resolveCustomerDisplayName(conn, transactionId);
            String barcodePayload = ReceiptCode39.parkingBarcodePayload(transactionId);
            final String receiptText = buildPrintableReceipt(tableModel, row, amount, alreadyPrinted, customerName, barcodePayload, paymentMethod,
                    cashTendered, changeAmount);
            markReceiptGenerated(conn, transactionId);
            final boolean allowPrint = !alreadyPrinted;
            JPanel preview = buildReceiptPreviewPanel(tableModel, row, amount, alreadyPrinted, customerName, barcodePayload, paymentMethod,
                    cashTendered, changeAmount, 48, 2);
            boolean wantPrint = showReceiptDialog(parent, preview, allowPrint);
            if (allowPrint && wantPrint) {
                printReceipt(parent, receiptText, transactionId, barcodePayload);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Failed to generate receipt: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            ConnectionConfig.close(conn);
        }
        if (afterDialogClosed != null) {
            SwingUtilities.invokeLater(afterDialogClosed);
        }
    }

    private static void printReceipt(Component parent, String receiptText, int transactionId, String barcodePayload) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Parking Receipt");
        job.setPrintable((Graphics g, PageFormat pf, int pageIndex) -> {
                if (pageIndex > 0) {
                    return Printable.NO_SUCH_PAGE;
                }
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                double pageWidth = pf.getImageableWidth();
                double pageHeight = pf.getImageableHeight();
                Font font = new Font(Font.MONOSPACED, Font.PLAIN, 10);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics(font);
                float lineHeight = fm.getHeight();
                String[] lines = receiptText.split("\n");
                float barcodeBlock = lineHeight * 8;
                float totalHeight = lines.length * lineHeight + barcodeBlock;
                float y = (float) Math.max(lineHeight, (pageHeight / 2.0 - totalHeight / 2.0 + lineHeight));
                for (String line : lines) {
                    int lineWidth = fm.stringWidth(line);
                    float x = (float) Math.max(0, (pageWidth - lineWidth) / 2.0);
                    g2.drawString(line, x, y);
                    y += lineHeight;
                }
                y += lineHeight * 0.35f;
                try {
                    BufferedImage bc = ReceiptCode39.toBufferedImage(barcodePayload, 32, 2);
                    double maxW = pageWidth - 40;
                    double scale = Math.min(1.0, maxW / bc.getWidth());
                    int dw = Math.max(1, (int) (bc.getWidth() * scale));
                    int dh = Math.max(1, (int) (bc.getHeight() * scale));
                    int dx = (int) Math.max(0, (pageWidth - dw) / 2.0);
                    g2.drawImage(bc, dx, (int) y, dw, dh, null);
                } catch (RuntimeException ignored) {
                    String ref = "Barcode: " + barcodePayload.toUpperCase(Locale.US);
                    float x = (float) Math.max(0, (pageWidth - fm.stringWidth(ref)) / 2.0);
                    g2.drawString(ref, x, y);
                }
                return Printable.PAGE_EXISTS;
            });
        if (job.printDialog()) {
            try {
                job.print();
                markReceiptPrinted(parent, transactionId);
            } catch (PrinterException printEx) {
                JOptionPane.showMessageDialog(parent, "Could not print receipt: " + printEx.getMessage(),
                        "Print Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Marks that the cashier opened receipt generation (preview). The customer sees View Receipts only after {@link #markReceiptPrinted}.
     */
    private static void markReceiptGenerated(Connection conn, int transactionId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE payments SET receipt_generated = 1 WHERE payment_id = ("
                        + "SELECT payment_id FROM payments WHERE transaction_id = ? ORDER BY payment_id DESC LIMIT 1)")) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        }
    }

    private static void markReceiptPrinted(Component parent, int transactionId) {
        Connection conn = null;
        try {
            conn = ConnectionConfig.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE payments SET receipt_printed = 1 WHERE payment_id = (SELECT payment_id FROM payments WHERE transaction_id = ? ORDER BY payment_id DESC LIMIT 1)")) {
                ps.setInt(1, transactionId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Receipt printed but could not update record: " + e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
        } finally {
            ConnectionConfig.close(conn);
        }
    }
}
