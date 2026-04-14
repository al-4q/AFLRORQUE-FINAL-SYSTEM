package Configuration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Consistent “card” styling for {@link JOptionPane} forms and summary dialogs.
 */
public final class StyledDialog {

    private static final int PAD = 16;
    private static final Color STAT_MUTED = new Color(0x5c, 0x6b, 0x7a);
    private static final Color STAT_VALUE = new Color(0x1e, 0x2d, 0x4a);
    /** Soft tints for report rows */
    private static final Color TINT_TODAY = new Color(0xeb, 0xf5, 0xfd);
    private static final Color TINT_YESTERDAY = new Color(0xf4, 0xf6, 0xf8);
    private static final Color TINT_TOTAL = new Color(0xe0, 0xef, 0xfc);
    private static final Color TINT_DAILY_HERO = new Color(0xeb, 0xf5, 0xfd);
    private static final Color HEADER_SUB_ON_NAVY = new Color(0xa8, 0xc4, 0xe8);

    private StyledDialog() { }

    /** Philippine peso: ₱ prefix, comma thousands, two decimals (e.g. ₱1,400.00). */
    private static String formatPesoAmount(double amount) {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
        sym.setGroupingSeparator(',');
        sym.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("₱#,##0.00", sym);
        return df.format(amount);
    }

    private static boolean isInsideDesktopPane(Component parent) {
        return parent != null
                && SwingUtilities.getAncestorOfClass(JDesktopPane.class, parent) != null;
    }

    /**
     * {@link JOptionPane#showInternalConfirmDialog} / {@code showInternalMessageDialog} live
     * inside the {@link JDesktopPane}, so they cannot move like a normal window and are often
     * clipped. Use the enclosing {@link Frame} from {@link JOptionPane#getFrameForComponent}
     * so the modal {@code JDialog} is draggable and fully sized.
     */
    private static Component ownerForModalDialog(Component parent) {
        if (parent == null) {
            return null;
        }
        if (isInsideDesktopPane(parent)) {
            Frame f = JOptionPane.getFrameForComponent(parent);
            if (f != null) {
                return f;
            }
            Window w = SwingUtilities.getWindowAncestor(parent);
            if (w != null) {
                return w;
            }
        }
        return parent;
    }

    /** Form dialogs: card + label/input styling. */
    public static JPanel wrapCard(JComponent inner) {
        JPanel card = wrapCardBare(inner);
        styleLabelsRecursive(inner);
        return card;
    }

    /** Summary / custom layouts: card border only (does not reset title fonts). */
    public static JPanel wrapCardBare(JComponent inner) {
        return wrapCardBare(inner, true);
    }

    /**
     * @param clearChildPanelOpacity when false, leaves child {@link JPanel} opacity as set (for colored stat rows).
     */
    private static JPanel wrapCardBare(JComponent inner, boolean clearChildPanelOpacity) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(0xe2, 0xe8, 0xee));
        JPanel innerCard = new JPanel(new BorderLayout());
        innerCard.setBackground(AppTheme.CARD_BG);
        innerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.ACCENT, 2),
                new EmptyBorder(PAD, PAD, PAD, PAD)));
        inner.setOpaque(false);
        if (inner instanceof JPanel) {
            ((JPanel) inner).setOpaque(false);
        }
        innerCard.add(inner, BorderLayout.CENTER);
        card.setBorder(new EmptyBorder(8, 8, 8, 8));
        card.add(innerCard, BorderLayout.CENTER);
        if (clearChildPanelOpacity) {
            clearChildPanelsOpaque(inner);
        }
        styleInputsRecursive(inner);
        return card;
    }

    private static void clearChildPanelsOpaque(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setOpaque(false);
                clearChildPanelsOpaque((Container) comp);
            } else if (comp instanceof Container) {
                clearChildPanelsOpaque((Container) comp);
            }
        }
    }

    private static void styleLabelsRecursive(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel l = (JLabel) comp;
                String t = l.getText();
                if (t != null && t.trim().toLowerCase().startsWith("<html")) {
                    continue;
                }
                l.setFont(AppTheme.FONT_LABEL);
                l.setForeground(AppTheme.TEXT_PRIMARY);
            } else if (comp instanceof Container) {
                styleLabelsRecursive((Container) comp);
            }
        }
    }

    private static void styleInputsRecursive(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTextField && !(comp instanceof JPasswordField)) {
                JTextField tf = (JTextField) comp;
                tf.setFont(AppTheme.FONT_LABEL);
                tf.setForeground(AppTheme.TEXT_PRIMARY);
                tf.setBackground(Color.WHITE);
                tf.setCaretColor(AppTheme.TEXT_PRIMARY);
                tf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                        new EmptyBorder(6, 8, 6, 8)));
            } else if (comp instanceof JPasswordField) {
                JPasswordField pf = (JPasswordField) comp;
                pf.setFont(AppTheme.FONT_LABEL);
                pf.setForeground(AppTheme.TEXT_PRIMARY);
                pf.setBackground(Color.WHITE);
                pf.setCaretColor(AppTheme.TEXT_PRIMARY);
                pf.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppTheme.BORDER, 1),
                        new EmptyBorder(6, 8, 6, 8)));
            } else if (comp instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) comp;
                cb.setFont(AppTheme.FONT_LABEL);
                cb.setForeground(AppTheme.TEXT_PRIMARY);
                cb.setBackground(Color.WHITE);
                Component ed = cb.getEditor().getEditorComponent();
                if (ed instanceof JTextField) {
                    JTextField etf = (JTextField) ed;
                    etf.setForeground(AppTheme.TEXT_PRIMARY);
                    etf.setBackground(Color.WHITE);
                    etf.setCaretColor(AppTheme.TEXT_PRIMARY);
                    etf.setFont(AppTheme.FONT_LABEL);
                }
            } else if (comp instanceof Container) {
                styleInputsRecursive((Container) comp);
            }
        }
    }

    /**
     * OK/Cancel form dialog. Uses a real {@link JDialog} with an explicit button row so the
     * bar is never clipped (tall custom panels + {@link JOptionPane} can hide buttons on some L&amp;Fs).
     */
    public static int showFormConfirm(Component parent, JPanel formContent, String title) {
        final int[] result = new int[]{JOptionPane.CANCEL_OPTION};
        Component own = ownerForModalDialog(parent);
        Window win;
        if (own instanceof Window) {
            win = (Window) own;
        } else if (own != null) {
            win = SwingUtilities.getWindowAncestor(own);
        } else {
            win = null;
        }
        JDialog dlg;
        if (win instanceof Frame) {
            dlg = new JDialog((Frame) win, title, true);
        } else if (win instanceof Dialog) {
            dlg = new JDialog((Dialog) win, title, true);
        } else {
            dlg = new JDialog((Frame) null, title, true);
        }
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(wrapCard(formContent), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        btnRow.add(cancel);
        btnRow.add(ok);
        root.add(btnRow, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.getRootPane().setDefaultButton(ok);
        ok.addActionListener(e -> {
            result[0] = JOptionPane.OK_OPTION;
            dlg.dispose();
        });
        cancel.addActionListener(e -> {
            result[0] = JOptionPane.CANCEL_OPTION;
            dlg.dispose();
        });
        dlg.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                result[0] = JOptionPane.CANCEL_OPTION;
            }
        });
        root.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "styledFormCancel");
        root.getActionMap().put("styledFormCancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel.doClick();
            }
        });

        dlg.pack();
        dlg.setLocationRelativeTo(own);
        dlg.setVisible(true);
        return result[0];
    }

    public static void showPlainMessage(java.awt.Component parent, String title, String message) {
        JTextArea ta = new JTextArea(message);
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setFont(AppTheme.FONT_LABEL);
        ta.setForeground(AppTheme.TEXT_PRIMARY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setColumns(36);
        ta.setRows(0);
        ta.setBorder(new EmptyBorder(4, 0, 4, 0));
        Object msg = wrapCard(ta);
        JOptionPane.showMessageDialog(ownerForModalDialog(parent), msg, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void showAdminSalesReport(java.awt.Component parent, String title,
            String today, String yesterday,
            double todaySales, int todayCount,
            double yesterdaySales, int yesterdayCount,
            double totalSales, int totalCount) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(true);
        root.setBackground(Color.WHITE);

        JPanel headerBar = new JPanel(new BorderLayout(8, 0));
        headerBar.setBackground(AppTheme.NAV_BTN_BG);
        headerBar.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel head = new JLabel("Sales overview");
        head.setFont(new Font("Segoe UI", Font.BOLD, 20));
        head.setForeground(Color.WHITE);
        headerBar.add(head, BorderLayout.WEST);
        JLabel sub = new JLabel("Payments in the system");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(HEADER_SUB_ON_NAVY);
        headerBar.add(sub, BorderLayout.EAST);
        root.add(headerBar, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(true);
        body.setBackground(new Color(0xf5, 0xf7, 0xfa));
        body.setBorder(new EmptyBorder(18, 18, 18, 18));

        addStatBlock(body, "Today", today, todaySales, todayCount, TINT_TODAY, AppTheme.ACCENT);
        body.add(Box.createVerticalStrut(10));
        addStatBlock(body, "Yesterday", yesterday, yesterdaySales, yesterdayCount, TINT_YESTERDAY, STAT_VALUE);
        body.add(Box.createVerticalStrut(14));
        JPanel divider = new JPanel();
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, AppTheme.ACCENT));
        body.add(divider);
        body.add(Box.createVerticalStrut(14));
        addTotalAllTime(body, totalSales, totalCount, TINT_TOTAL);

        root.add(body, BorderLayout.CENTER);

        Object msg = wrapCardBare(root, false);
        JOptionPane.showMessageDialog(ownerForModalDialog(parent), msg, title, JOptionPane.PLAIN_MESSAGE);
    }

    private static void addTotalAllTime(JPanel root, double amount, int transactions, Color rowBg) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(true);
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, AppTheme.NAV_BTN_BG),
                        BorderFactory.createLineBorder(new Color(0xc5, 0xd5, 0xe8), 1)),
                new EmptyBorder(12, 14, 12, 14)));
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        JLabel top = new JLabel("Total (all time)");
        top.setFont(new Font("Segoe UI", Font.BOLD, 13));
        top.setForeground(AppTheme.NAV_BTN_BG);
        block.add(top, BorderLayout.NORTH);
        JLabel amt = new JLabel(formatPesoAmount(amount));
        amt.setFont(new Font("Segoe UI", Font.BOLD, 26));
        amt.setForeground(AppTheme.ACCENT);
        block.add(amt, BorderLayout.CENTER);
        JLabel cnt = new JLabel(transactions + " paid transaction" + (transactions == 1 ? "" : "s"));
        cnt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cnt.setForeground(STAT_MUTED);
        block.add(cnt, BorderLayout.SOUTH);
        root.add(block);
    }

    private static void addStatBlock(JPanel root, String label, String dateLine, double amount, int transactions,
            Color rowBg, Color amountColor) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(true);
        block.setBackground(rowBg);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, AppTheme.ACCENT),
                new EmptyBorder(12, 14, 12, 14)));
        block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel top = new JLabel(label + "  ·  " + dateLine);
        top.setFont(new Font("Segoe UI", Font.BOLD, 12));
        top.setForeground(STAT_MUTED);
        block.add(top, BorderLayout.NORTH);

        JLabel amt = new JLabel(formatPesoAmount(amount));
        amt.setFont(new Font("Segoe UI", Font.BOLD, 24));
        amt.setForeground(amountColor);
        block.add(amt, BorderLayout.CENTER);

        JLabel cnt = new JLabel(transactions + " paid transaction" + (transactions == 1 ? "" : "s"));
        cnt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cnt.setForeground(STAT_MUTED);
        block.add(cnt, BorderLayout.SOUTH);

        root.add(block);
    }

    public static void showDailySalesSummary(java.awt.Component parent, double total, int count) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(true);
        root.setBackground(Color.WHITE);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(AppTheme.NAV_BTN_BG);
        headerBar.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel head = new JLabel("Daily sales");
        head.setFont(new Font("Segoe UI", Font.BOLD, 20));
        head.setForeground(Color.WHITE);
        headerBar.add(head, BorderLayout.WEST);
        JLabel chip = new JLabel(" Cashier ");
        chip.setOpaque(true);
        chip.setBackground(AppTheme.ACCENT);
        chip.setForeground(Color.WHITE);
        chip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        chip.setBorder(new EmptyBorder(4, 10, 4, 10));
        headerBar.add(chip, BorderLayout.EAST);
        root.add(headerBar, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(true);
        body.setBackground(new Color(0xf5, 0xf7, 0xfa));
        body.setBorder(new EmptyBorder(20, 18, 22, 18));

        JLabel sub = new JLabel("All recorded payments");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(STAT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(sub);
        body.add(Box.createVerticalStrut(16));

        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(true);
        hero.setBackground(TINT_DAILY_HERO);
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x90, 0xc5, 0xec), 1),
                new EmptyBorder(20, 20, 20, 20)));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel amt = new JLabel(formatPesoAmount(total));
        amt.setFont(new Font("Segoe UI", Font.BOLD, 36));
        amt.setForeground(AppTheme.ACCENT);
        amt.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(amt);
        hero.add(Box.createVerticalStrut(6));
        JLabel amtLbl = new JLabel("Total amount");
        amtLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        amtLbl.setForeground(STAT_MUTED);
        amtLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(amtLbl);
        body.add(hero);
        body.add(Box.createVerticalStrut(14));

        JLabel cnt = new JLabel(count + " payment" + (count == 1 ? "" : "s") + " on file");
        cnt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cnt.setForeground(AppTheme.NAV_BTN_BG);
        cnt.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(cnt);

        root.add(body, BorderLayout.CENTER);

        Object msg = wrapCardBare(root, false);
        JOptionPane.showMessageDialog(ownerForModalDialog(parent), msg, "Daily Sales", JOptionPane.PLAIN_MESSAGE);
    }

    /** Admin parking screen: same card style as cashier daily sales, with Admin chip and date-scoped copy. */
    public static void showAdminDailySalesSummary(java.awt.Component parent, String dateIso, double total, int count) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(true);
        root.setBackground(Color.WHITE);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(AppTheme.NAV_BTN_BG);
        headerBar.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel head = new JLabel("Daily sales");
        head.setFont(new Font("Segoe UI", Font.BOLD, 20));
        head.setForeground(Color.WHITE);
        headerBar.add(head, BorderLayout.WEST);
        JLabel chip = new JLabel(" Admin ");
        chip.setOpaque(true);
        chip.setBackground(AppTheme.ACCENT);
        chip.setForeground(Color.WHITE);
        chip.setFont(new Font("Segoe UI", Font.BOLD, 11));
        chip.setBorder(new EmptyBorder(4, 10, 4, 10));
        headerBar.add(chip, BorderLayout.EAST);
        root.add(headerBar, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(true);
        body.setBackground(new Color(0xf5, 0xf7, 0xfa));
        body.setBorder(new EmptyBorder(20, 18, 22, 18));

        JLabel sub = new JLabel("Payments for " + dateIso);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(STAT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(sub);
        body.add(Box.createVerticalStrut(16));

        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(true);
        hero.setBackground(TINT_DAILY_HERO);
        hero.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x90, 0xc5, 0xec), 1),
                new EmptyBorder(20, 20, 20, 20)));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel amt = new JLabel(formatPesoAmount(total));
        amt.setFont(new Font("Segoe UI", Font.BOLD, 36));
        amt.setForeground(AppTheme.ACCENT);
        amt.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(amt);
        hero.add(Box.createVerticalStrut(6));
        JLabel amtLbl = new JLabel("Total amount (today)");
        amtLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        amtLbl.setForeground(STAT_MUTED);
        amtLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.add(amtLbl);
        body.add(hero);
        body.add(Box.createVerticalStrut(14));

        JLabel cnt = new JLabel(count + " payment" + (count == 1 ? "" : "s") + " today");
        cnt.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cnt.setForeground(AppTheme.NAV_BTN_BG);
        cnt.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(cnt);

        root.add(body, BorderLayout.CENTER);

        Object msg = wrapCardBare(root, false);
        JOptionPane.showMessageDialog(ownerForModalDialog(parent), msg, "Daily Sales", JOptionPane.PLAIN_MESSAGE);
    }
}
