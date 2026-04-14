package Configuration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Shared aesthetic theme for Parking Slotwise. Use for consistent, presentable UI
 * and to keep text fully visible (no clipping).
 */
public final class AppTheme {

    private AppTheme() { }

    // Sidebar & navigation
    public static final Color SIDEBAR_BG = new Color(0x16, 0x21, 0x3e);
    public static final Color NAV_BTN_BG = new Color(0x1e, 0x2d, 0x4a);
    public static final Color NAV_BTN_HOVER = new Color(0x34, 0x98, 0xdb);
    public static final Color CONTENT_BG = new Color(0xec, 0xf0, 0xf1);
    public static final Color CARD_BG = Color.WHITE;

    // Text
    // Use solid black for primary and muted text so it is always clearly visible
    public static final Color TEXT_PRIMARY = Color.BLACK;
    public static final Color TEXT_ON_NAV = Color.WHITE;
    public static final Color TEXT_MUTED = Color.BLACK;

    // Accent
    public static final Color ACCENT = new Color(0x34, 0x98, 0xdb);
    public static final Color BORDER = new Color(0xbd, 0xc3, 0xc7);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_NAV = new Font("Segoe UI", Font.BOLD, 11);

    /** Style a nav button panel and its label for full text visibility. */
    public static JPanel createNavPanel(String text) {
        JPanel p = new JPanel();
        p.setBackground(NAV_BTN_BG);
        p.setBorder(new EmptyBorder(4, 8, 4, 8));
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_NAV);
        lbl.setForeground(TEXT_ON_NAV);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        p.add(lbl);
        return p;
    }

    /**
     * {@link #createNavPanel} puts a {@link JLabel} on top; clicks often hit the label, not the panel.
     * Attach the same handler to the panel and its children so nav buttons work reliably.
     */
    public static void wireNavPanelClick(JPanel navPanel, Runnable onClick) {
        if (navPanel == null || onClick == null) {
            return;
        }
        Cursor hand = new Cursor(Cursor.HAND_CURSOR);
        navPanel.setCursor(hand);
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Defer so closing a JOptionPane (OK/X) cannot re-dispatch this click to the
                    // same nav button and run the action twice (e.g. Calculate Fee after "select row" warning).
                    SwingUtilities.invokeLater(onClick::run);
                }
            }
        };
        navPanel.addMouseListener(ma);
        for (Component c : navPanel.getComponents()) {
            c.setCursor(hand);
            c.addMouseListener(ma);
        }
    }

    /** Apply theme to a JTable so cell text is fully visible (row height, font, grid). */
    public static void styleTable(JTable table) {
        table.setRowHeight(Math.max(table.getRowHeight(), 24));
        table.setFont(FONT_LABEL);
        table.setForeground(TEXT_PRIMARY);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.getTableHeader().setFont(FONT_NAV);
        // Keep header background, but force header text to solid black for visibility
        table.getTableHeader().setBackground(NAV_BTN_BG);
        table.getTableHeader().setForeground(Color.BLACK);
    }
}
