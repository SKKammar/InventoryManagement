import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbDebug {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://db.dgufnkbmwaxtjtpdvvcn.supabase.co:5432/postgres";
        String user = "postgres";
        String password = "7975433262#santu";

        try {
            System.out.println("Connecting to database...");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connected!");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, username, email, password FROM users");
            
            System.out.println("--- USERS TABLE ---");
            int count = 0;
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id"));
                System.out.println("Username: " + rs.getString("username"));
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Password Hash: " + rs.getString("password"));
                System.out.println("-------------------");
                count++;
            }
            if (count == 0) {
                System.out.println("No users found in database!");
            }
            
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
