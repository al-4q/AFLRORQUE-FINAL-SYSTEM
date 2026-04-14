package Configuration;

/*
 * Code 39 encoding constants and width expansion derived from ZXing (Apache License 2.0).
 * https://github.com/zxing/zxing
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders Code 39 barcodes for on-screen receipts and printing.
 * Encoding tables and algorithm derived from ZXing (Apache License 2.0).
 *
 * @see <a href="https://github.com/zxing/zxing">ZXing</a>
 */
public final class ReceiptCode39 {

    private ReceiptCode39() { }

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%";

    private static final int[] CHARACTER_ENCODINGS = {
            0x034, 0x121, 0x061, 0x160, 0x031, 0x130, 0x070, 0x025, 0x124, 0x064,
            0x109, 0x049, 0x148, 0x019, 0x118, 0x058, 0x00D, 0x10C, 0x04C, 0x01C,
            0x103, 0x043, 0x142, 0x013, 0x112, 0x052, 0x007, 0x106, 0x046, 0x016,
            0x181, 0x0C1, 0x1C0, 0x091, 0x190, 0x0D0, 0x085, 0x184, 0x0C4, 0x0A8,
            0x0A2, 0x08A, 0x02A
    };

    private static final int ASTERISK_ENCODING = 0x094;

    private static void toWidths(int a, int[] toReturn) {
        for (int i = 0; i < 9; i++) {
            int temp = a & (1 << (8 - i));
            toReturn[i] = temp == 0 ? 1 : 2;
        }
    }

    private static void appendPattern(List<Boolean> target, int[] pattern, boolean startBlack) {
        boolean black = startBlack;
        for (int len : pattern) {
            for (int j = 0; j < len; j++) {
                target.add(black);
            }
            black = !black;
        }
    }

    /**
     * @param contents uppercase Code 39 payload without start/stop (e.g. {@code T52}); A–Z, 0–9, etc.
     */
    public static boolean[] encode(String contents) {
        String c = contents.toUpperCase(Locale.US).trim();
        if (c.isEmpty()) {
            return new boolean[0];
        }
        for (int i = 0; i < c.length(); i++) {
            if (ALPHABET.indexOf(c.charAt(i)) < 0) {
                throw new IllegalArgumentException("Unsupported barcode character: " + c.charAt(i));
            }
        }
        int[] widths = new int[9];
        List<Boolean> bits = new ArrayList<>();
        toWidths(ASTERISK_ENCODING, widths);
        appendPattern(bits, widths, true);
        int[] narrowWhite = {1};
        appendPattern(bits, narrowWhite, false);
        for (int i = 0; i < c.length(); i++) {
            int idx = ALPHABET.indexOf(c.charAt(i));
            toWidths(CHARACTER_ENCODINGS[idx], widths);
            appendPattern(bits, widths, true);
            appendPattern(bits, narrowWhite, false);
        }
        toWidths(ASTERISK_ENCODING, widths);
        appendPattern(bits, widths, true);
        boolean[] out = new boolean[bits.size()];
        for (int i = 0; i < bits.size(); i++) {
            out[i] = bits.get(i);
        }
        return out;
    }

    /** Human-readable payload for Code 39 (letters + digits). */
    public static String parkingBarcodePayload(int transactionId) {
        return "T" + transactionId;
    }

    public static BufferedImage toBufferedImage(String contents, int barHeightPx, int modulePx) {
        boolean[] row = encode(contents);
        int w = row.length * modulePx + 2 * modulePx * 10;
        int h = barHeightPx + modulePx * 14;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        int x = modulePx * 10;
        int barTop = modulePx * 4;
        for (int i = 0; i < row.length; i++) {
            if (row[i]) {
                g.fillRect(x, barTop, modulePx, barHeightPx);
            }
            x += modulePx;
        }
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        String label = contents.toUpperCase(Locale.US);
        int sw = g.getFontMetrics().stringWidth(label);
        g.drawString(label, Math.max(modulePx * 4, (w - sw) / 2), h - modulePx * 4);
        g.dispose();
        return img;
    }

    /** Draw barcode at (x,y); returns total height used (including caption). */
    public static int paint(Graphics2D g, int x, int y, String contents, int barHeightPx, int modulePx) {
        BufferedImage img = toBufferedImage(contents, barHeightPx, modulePx);
        g.drawImage(img, x, y, null);
        return img.getHeight();
    }
}
