package tw.com.stockplatform.member.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 密碼雜湊與比對（BCrypt）。
 * <p>
 * 採 favre BCrypt 而非 Spring Security PasswordEncoder，避免 Wave 1 強耦合 Security 框架。
 */
@Component
public class PasswordEncoder {

    private static final int COST = 12;

    public String encode(String rawPassword) {
        return BCrypt.withDefaults().hashToString(COST, rawPassword.toCharArray());
    }

    public boolean matches(String rawPassword, String hashed) {
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), hashed).verified;
    }
}
