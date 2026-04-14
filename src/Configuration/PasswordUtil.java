package Configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Simple password hashing utility.
 * Uses SHA-256 + Base64 for this project.
 */
public final class PasswordUtil {

    private PasswordUtil() { }

    public static String hashPassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard JVMs
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        String recalculated = hashPassword(plainPassword);
        return hashedPassword.equals(recalculated);
    }
}

