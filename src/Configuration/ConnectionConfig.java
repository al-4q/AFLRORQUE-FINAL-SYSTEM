package Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionConfig {

    private static final String DRIVER = "org.sqlite.JDBC";
    /**
     * This project ships with a pre-existing database file in the project root.
     * Prefer that file to avoid accidentally creating a new empty DB elsewhere.
     */
    private static final String ROOT_DB_NAME = "parking.db";
    /**
     * Fallback location/name if the root DB doesn't exist (kept for compatibility).
     */
    private static final String FALLBACK_DB_FOLDER = "database";
    private static final String FALLBACK_DB_NAME = "transportation.db";

    private static volatile boolean driverChecked = false;
    /** Ensured once per JVM run so that deleting the Walk-in user in Settings is not reverted on next refresh. */
    private static volatile boolean walkInUserEnsured = false;

    private static void ensureDriverLoaded() throws SQLException {
        if (driverChecked) return;
        synchronized (ConnectionConfig.class) {
            if (driverChecked) return;
            try {
                // Not strictly required for newer JDBC, but keeps older setups working.
                Class.forName(DRIVER);
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "SQLite JDBC driver not found on the classpath. " +
                        "In NetBeans: Right-click project -> Properties -> Libraries -> Add JAR/Folder " +
                        "and add sqlite-jdbc-3.30.1.jar.",
                        e
                );
            } finally {
                driverChecked = true;
            }
        }
    }

    public static String getDatabasePath() {
        String base = System.getProperty("user.dir");
        // 1) Prefer the DB that already exists in the project root (parking.db)
        File rootDb = new File(base, ROOT_DB_NAME);
        if (rootDb.exists() && rootDb.isFile()) {
            return rootDb.getAbsolutePath();
        }

        // 2) Fall back to the legacy location (database/transportation.db)
        File dir = new File(base, FALLBACK_DB_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, FALLBACK_DB_NAME).getAbsolutePath();
    }

    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        String url = "jdbc:sqlite:" + getDatabasePath();
        Connection conn = DriverManager.getConnection(url);
        initDatabase(conn);
        return conn;
    }

    // Parking SlotWise schema (users, parking_slots, vehicles, reservations, parking_transactions, payments, reports)
    private static final int DEFAULT_USER_ID = 1;

    private static void initDatabase(Connection conn) {
        try (Statement st = conn.createStatement()) {
            // Drop legacy tables (old schema)
            st.execute("DROP TABLE IF EXISTS payment");
            st.execute("DROP TABLE IF EXISTS park");
            st.execute("DROP TABLE IF EXISTS cars");
            st.execute("DROP TABLE IF EXISTS slots");
            st.execute("DROP TABLE IF EXISTS user_cars");
            st.execute("DROP TABLE IF EXISTS user_payments");
            st.execute("DROP TABLE IF EXISTS routes");
            st.execute("DROP TABLE IF EXISTS bookings");
            st.execute("DROP TABLE IF EXISTS receipts");

            // Migrate users table if it has old schema (u_id instead of user_id)
            try {
                st.executeQuery("SELECT user_id FROM users LIMIT 1");
            } catch (SQLException e) {
                st.execute("DROP TABLE IF EXISTS users");
            }

            // 1. Users table - Admin, Cashier, User login
            st.execute(
                "CREATE TABLE IF NOT EXISTS users ("
                + "user_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL, "
                + "role TEXT NOT NULL DEFAULT 'user', "
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP)"
            );

            // 2. Parking slots - Add, Edit, Delete, Slot Status (Admin)
            st.execute(
                "CREATE TABLE IF NOT EXISTS parking_slots ("
                + "slot_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "slot_number TEXT NOT NULL, "
                + "slot_type TEXT NOT NULL DEFAULT 'Car', "
                + "status TEXT NOT NULL DEFAULT 'Available', "
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP)"
            );

            // 3. Vehicles - vehicle details of users parking
            st.execute(
                "CREATE TABLE IF NOT EXISTS vehicles ("
                + "vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "plate_number TEXT NOT NULL, "
                + "vehicle_type TEXT NOT NULL, "
                + "owner_name TEXT NOT NULL)"
            );

            // 4. Reservations - Reserve Slot (User Dashboard)
            st.execute(
                "CREATE TABLE IF NOT EXISTS reservations ("
                + "reservation_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "slot_id INTEGER NOT NULL, "
                + "reservation_time TEXT NOT NULL, "
                + "status TEXT NOT NULL DEFAULT 'Active', "
                + "customer_name TEXT, "
                + "FOREIGN KEY (user_id) REFERENCES users (user_id), "
                + "FOREIGN KEY (slot_id) REFERENCES parking_slots (slot_id))"
            );
            ensureColumn(st, "reservations", "customer_name", "TEXT");
            ensureColumn(st, "reservations", "valid_until", "TEXT");
            ensureColumn(st, "reservations", "plate_number", "TEXT");

            // 5. Parking transactions - Park Vehicle, Parking Time, Slot Status (Cashier, Transactions, Parking history)
            st.execute(
                "CREATE TABLE IF NOT EXISTS parking_transactions ("
                + "transaction_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "vehicle_id INTEGER NOT NULL, "
                + "slot_id INTEGER NOT NULL, "
                + "time_in TEXT NOT NULL, "
                + "time_out TEXT, "
                + "parking_fee REAL, "
                + "status TEXT NOT NULL DEFAULT 'Active', "
                + "FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id), "
                + "FOREIGN KEY (slot_id) REFERENCES parking_slots (slot_id))"
            );

            // 6. Payments - Cashier: payment and receipts (Generate Receipt, Daily Sales, Payment History)
            st.execute(
                "CREATE TABLE IF NOT EXISTS payments ("
                + "payment_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "transaction_id INTEGER NOT NULL, "
                + "cashier_id INTEGER NOT NULL, "
                + "amount REAL NOT NULL, "
                + "payment_date TEXT NOT NULL, "
                + "FOREIGN KEY (transaction_id) REFERENCES parking_transactions (transaction_id), "
                + "FOREIGN KEY (cashier_id) REFERENCES users (user_id))"
            );
            try {
                st.execute("ALTER TABLE payments ADD COLUMN receipt_printed INTEGER DEFAULT 0");
            } catch (SQLException ignored) {
                // Column already exists
            }
            ensureColumn(st, "payments", "receipt_printed", "INTEGER DEFAULT 0");
            try {
                st.execute("UPDATE payments SET receipt_printed = 0 WHERE receipt_printed IS NULL");
            } catch (SQLException ignored) { }
            ensureColumn(st, "payments", "receipt_generated", "INTEGER DEFAULT 0");
            ensureColumn(st, "payments", "payment_method", "TEXT DEFAULT 'Cash'");
            ensureColumn(st, "payments", "cash_tendered", "REAL");
            ensureColumn(st, "payments", "change_amount", "REAL");
            try {
                // Cashiers who already printed had effectively issued the receipt to the customer
                st.execute("UPDATE payments SET receipt_generated = 1 WHERE COALESCE(receipt_printed, 0) = 1");
            } catch (SQLException ignored) { }

            // 7. Reports (Optional) - Admin reports
            st.execute(
                "CREATE TABLE IF NOT EXISTS reports ("
                + "report_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "report_date TEXT NOT NULL, "
                + "total_transactions INTEGER NOT NULL DEFAULT 0, "
                + "total_income REAL NOT NULL DEFAULT 0)"
            );

            ensureColumn(st, "users", "status", "TEXT DEFAULT 'approved'");
            try {
                st.execute("UPDATE users SET status = 'approved' WHERE status IS NULL OR status = ''");
            } catch (SQLException ignored) { }
            resetSequenceIfEmpty(st, "users", "user_id", DEFAULT_USER_ID);
            seedDefaultUsersIfEmpty(conn);
            ensureWalkInUser(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database tables.", e);
        }
    }

    /** Walk-in user for reservations when admin/cashier enters a name manually. */
    public static int getWalkInUserId(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = 'walkin' LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt("user_id");
        }
        return -1;
    }

    private static void ensureWalkInUser(Connection conn) throws SQLException {
        if (walkInUserEnsured) return;
        synchronized (ConnectionConfig.class) {
            if (walkInUserEnsured) return;
            if (getWalkInUserId(conn) > 0) {
                walkInUserEnsured = true;
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (name, username, password, role, status) VALUES ('Walk-in', 'walkin', ?, 'user', 'approved')")) {
                ps.setString(1, PasswordUtil.hashPassword("walkin"));
                ps.executeUpdate();
            }
            walkInUserEnsured = true;
        }
    }

    private static void seedDefaultUsersIfEmpty(Connection conn) throws SQLException {
        try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM users")) {
            if (!rs.next() || rs.getInt(1) > 0) return;
        }
        String hashAdmin = PasswordUtil.hashPassword("admin");
        String hashCashier = PasswordUtil.hashPassword("cashier");
        String hashUser = PasswordUtil.hashPassword("user");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (name, username, password, role) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, "Admin");
            ps.setString(2, "admin");
            ps.setString(3, hashAdmin);
            ps.setString(4, "admin");
            ps.executeUpdate();
            ps.setString(1, "Cashier");
            ps.setString(2, "cashier1");
            ps.setString(3, hashCashier);
            ps.setString(4, "cashier");
            ps.executeUpdate();
            ps.setString(1, "Juan");
            ps.setString(2, "juan123");
            ps.setString(3, hashUser);
            ps.setString(4, "user");
            ps.executeUpdate();
        }
    }

    private static void ensureColumn(Statement st, String table, String column, String typeDef) {
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return;
            }
        } catch (SQLException e) { return; }
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + typeDef);
        } catch (SQLException ignored) { }
    }

    private static void resetSequenceIfEmpty(Statement st, String tableName, String idColumn, int defaultStartId) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next() && rs.getInt(1) == 0) {
                st.execute("DELETE FROM sqlite_sequence WHERE name = '" + tableName + "'");
                st.execute("INSERT INTO sqlite_sequence (name, seq) VALUES ('" + tableName + "', " + (defaultStartId - 1) + ")");
            }
        }
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) conn.close();
            } catch (SQLException ignored) { }
        }
    }
}
