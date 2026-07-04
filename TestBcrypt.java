import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String raw = "admin123";
        String encoded = "$2a$10$elSAkQfmwcBhhWpdWGmYY.A0OOICxb8UpYASslRzifhxrbC1hkKZy";
        System.out.println("Matches? " + encoder.matches(raw, encoded));
    }
}
