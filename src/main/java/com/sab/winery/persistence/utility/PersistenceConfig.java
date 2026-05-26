package com.sab.winery.persistence.utility;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Локальна БД SQLite {@code winery.db}: виноробні, програми турів, сесії, гості, бронювання, записи
 * візитів, асортимент вина.
 */
public final class PersistenceConfig {

    private static final HikariDataSource DS;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + resolveDbPath());
        config.addDataSourceProperty("cachePrepStmts", "true");
        DS = new HikariDataSource(config);
    }

    private PersistenceConfig() {}

    private static String resolveDbPath() {
        String[] candidates = {
            "./resources/winery.db", "src/main/resources/winery.db", "./winery.db"
        };
        for (String path : candidates) {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null && parent.exists()) {
                return path;
            }
        }
        return "./winery.db";
    }

    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    public static void initSchema() {
        try (Connection con = getConnection();
                Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS app_users ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "username TEXT NOT NULL UNIQUE,"
                            + "password_hash TEXT NOT NULL,"
                            + "role TEXT NOT NULL,"
                            + "full_name TEXT)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS wineries ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "name TEXT NOT NULL)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS tour_programs ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "winery_id INTEGER NOT NULL,"
                            + "title TEXT NOT NULL,"
                            + "description TEXT,"
                            + "duration_minutes INTEGER NOT NULL DEFAULT 60,"
                            + "default_max_guests INTEGER NOT NULL DEFAULT 20,"
                            + "image_path TEXT,"
                            + "active INTEGER NOT NULL DEFAULT 1,"
                            + "FOREIGN KEY(winery_id) REFERENCES wineries(id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_tour_programs_winery ON tour_programs(winery_id)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS tour_sessions ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "program_id INTEGER NOT NULL,"
                            + "start_time TEXT NOT NULL,"
                            + "capacity INTEGER NOT NULL,"
                            + "status TEXT NOT NULL DEFAULT 'SCHEDULED',"
                            + "FOREIGN KEY(program_id) REFERENCES tour_programs(id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_tour_sessions_program ON tour_sessions(program_id)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS guest_profiles ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "name TEXT NOT NULL,"
                            + "email TEXT NOT NULL,"
                            + "phone TEXT NOT NULL)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_guest_email ON guest_profiles(email)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS reservations ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "session_id INTEGER NOT NULL,"
                            + "guest_id INTEGER NOT NULL,"
                            + "party_size INTEGER NOT NULL,"
                            + "status TEXT NOT NULL DEFAULT 'CONFIRMED',"
                            + "created_at TEXT NOT NULL,"
                            + "FOREIGN KEY(session_id) REFERENCES tour_sessions(id),"
                            + "FOREIGN KEY(guest_id) REFERENCES guest_profiles(id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_reservations_session ON reservations(session_id)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS visit_records ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "reservation_id INTEGER NOT NULL UNIQUE,"
                            + "attended_count INTEGER NOT NULL,"
                            + "notes TEXT,"
                            + "status TEXT NOT NULL,"
                            + "FOREIGN KEY(reservation_id) REFERENCES reservations(id))");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS wine_products ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "winery_id INTEGER NOT NULL,"
                            + "name TEXT NOT NULL,"
                            + "category TEXT,"
                            + "price REAL NOT NULL,"
                            + "stock_units INTEGER NOT NULL DEFAULT 0,"
                            + "image_path TEXT,"
                            + "active INTEGER NOT NULL DEFAULT 1,"
                            + "FOREIGN KEY(winery_id) REFERENCES wineries(id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_wine_winery ON wine_products(winery_id)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS club_memberships ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "guest_id INTEGER NOT NULL,"
                            + "tier TEXT NOT NULL,"
                            + "valid_until TEXT NOT NULL,"
                            + "discount_percent INTEGER NOT NULL DEFAULT 5,"
                            + "active INTEGER NOT NULL DEFAULT 1,"
                            + "FOREIGN KEY(guest_id) REFERENCES guest_profiles(id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_club_guest ON club_memberships(guest_id)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS addon_services ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "name TEXT NOT NULL UNIQUE,"
                            + "kind TEXT NOT NULL,"
                            + "price_per_unit REAL NOT NULL,"
                            + "active INTEGER NOT NULL DEFAULT 1)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS visit_charges ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "reservation_id INTEGER NOT NULL,"
                            + "addon_service_id INTEGER NOT NULL,"
                            + "quantity INTEGER NOT NULL DEFAULT 1,"
                            + "usage_date_time TEXT NOT NULL,"
                            + "paid INTEGER NOT NULL DEFAULT 0,"
                            + "FOREIGN KEY(reservation_id) REFERENCES reservations(id),"
                            + "FOREIGN KEY(addon_service_id) REFERENCES addon_services(id))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_visit_charge_res ON visit_charges(reservation_id)");

            seedIfEmpty(con);
            seedAddonServicesIfEmpty(con);
            seedDefaultAdminIfMissing(con);
        } catch (SQLException e) {
            throw new RuntimeException("Не вдалося ініціалізувати базу даних winery", e);
        }
    }

    private static void seedAddonServicesIfEmpty(Connection con) throws SQLException {
        try (var st = con.createStatement();
                var rs = st.executeQuery("SELECT COUNT(*) FROM addon_services")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        Object[][] services = {
            {"Розширена дегустація (+3 зразки)", "TASTING_EXTRA", 280.0},
            {"Сирно-м'ясна тарілка", "SNACK_PLATTER", 350.0},
            {"Сувенірна пляшка", "SOUVENIR_BOTTLE", 420.0},
            {"Трансфер з міста", "TRANSFER", 600.0},
            {"Майстер-клас сомельє", "MASTERCLASS", 750.0}
        };
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO addon_services(name, kind, price_per_unit, active) VALUES(?,?,?,1)")) {
            for (Object[] s : services) {
                ps.setString(1, (String) s[0]);
                ps.setString(2, (String) s[1]);
                ps.setDouble(3, (Double) s[2]);
                ps.executeUpdate();
            }
        }
    }

    private static void seedDefaultAdminIfMissing(Connection con) throws SQLException {
        try (PreparedStatement chk = con.prepareStatement("SELECT 1 FROM app_users WHERE lower(username)=lower(?)")) {
            chk.setString(1, "admin");
            try (var rs = chk.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO app_users(username, password_hash, role, full_name) VALUES(?,?,?,?)")) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.sha256("admin123"));
            ps.setString(3, "ADMIN");
            ps.setString(4, "Адміністратор системи");
            ps.executeUpdate();
        }
    }

    private static void seedIfEmpty(Connection con) throws SQLException {
        try (var chk = con.createStatement();
                var rs = chk.executeQuery("SELECT COUNT(*) FROM wineries")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        try (Statement st = con.createStatement()) {
            st.executeUpdate("INSERT INTO wineries(name) VALUES ('Крафтова виноробня «Горизонт»')");

            st.executeUpdate(
                    "INSERT INTO tour_programs(winery_id, title, description, duration_minutes, default_max_guests, active) "
                            + "VALUES (1, 'Дегустація «Три купажі»', 'Огляд виробництва та три зразки вина.', 90, 16, 1)");

            st.executeUpdate(
                    "INSERT INTO tour_sessions(program_id, start_time, capacity, status) "
                            + "VALUES (1, '2026-06-15T11:00:00', 16, 'SCHEDULED')");
            st.executeUpdate(
                    "INSERT INTO tour_sessions(program_id, start_time, capacity, status) "
                            + "VALUES (1, '2026-06-22T15:00:00', 12, 'SCHEDULED')");

            st.executeUpdate(
                    "INSERT INTO wine_products(winery_id, name, category, price, stock_units, active) "
                            + "VALUES (1, 'Купаж «Горизонт» 2023', 'Червоне сухе', 420.0, 120, 1)");
            st.executeUpdate(
                    "INSERT INTO wine_products(winery_id, name, category, price, stock_units, active) "
                            + "VALUES (1, 'Рислінг 2024', 'Біле напівсолодке', 310.0, 85, 1)");
        }
    }
}
