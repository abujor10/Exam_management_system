import db.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    // INPUT: Command-line arguments -> ACTION: Validates DB connectivity and queries user count -> OUTPUT: Console diagnostic message
    public static void main(String[] args) {
        System.out.println("Testing database connection...");
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total_users FROM users")) {

            if (rs.next()) {
                int count = rs.getInt("total_users");
                System.out.println("Connected successfully!");
                System.out.println("Database verified. Total registered users: " + count);
            }
        } catch (Exception e) {
            System.err.println("Connection Failed!");
            e.printStackTrace();
        }
    }
}
