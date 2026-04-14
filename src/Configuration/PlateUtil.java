package Configuration;

/**
 * Plate number validation for reservations:
 * must be exactly 3 letters followed by 4 digits.
 * Example valid: ABC1234, PAR1234
 */
public final class PlateUtil {

    private PlateUtil() { }

    /**
     * Normalizes plate for display/storage: trim and convert to uppercase.
     */
    public static String normalize(String plate) {
        if (plate == null) return "";
        return plate.trim().toUpperCase();
    }

    /**
     * Validates plate for reservation:
     * - exactly 7 characters
     * - first 3 must be letters A–Z
     * - last 4 must be digits 0–9
     *
     * @param plate the raw plate string (will be normalized before validation)
     * @return null if valid, or an error message if invalid
     */
    public static String validate(String plate) {
        String normalized = normalize(plate);
        if (normalized.isEmpty()) {
            return "Plate number is required.";
        }
        if (!normalized.matches("^[A-Z]{3}[0-9]{4}$")) {
            return "Plate number must be 3 letters followed by 4 numbers (e.g. ABC1234).";
        }
        return null;
    }

    /**
     * Returns normalized plate if valid, or null and leaves validation message to caller.
     */
    public static String normalizeAndValidate(String plate) {
        String normalized = normalize(plate);
        return validate(plate) == null ? normalized : null;
    }
}
