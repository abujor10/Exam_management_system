package db;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static String host = "localhost";

    // সতর্কতা: XAMPP কন্ট্রোল প্যানেলে MySQL যে পোর্টে চলছে (3306 বা 3307), সেই পোর্টটি এখানে দিন
    private static final String PORT = "3307"; // যদি 3307 পোর্টে চলে তবে এখানে "3307" লিখুন

    private static final String DB_NAME = "exam_system_db";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // XAMPP-এ পাসওয়ার্ড সেট করা থাকলে এখানে লিখুন

    static {
        File configFile = new File("server_ip.txt");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                host = props.getProperty("host", "localhost").trim();
            } catch (Exception e) {
                host = "localhost";
            }
        }
    }

    // INPUT: None -> ACTION: Establishes and returns a JDBC connection -> OUTPUT: Active Connection object
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in classpath.", e);
        }

        String url = "jdbc:mysql://" + host + ":" + PORT + "/" + DB_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    public static void setHost(String customHost) {
        host = customHost;
    }
}