package tw.com.stockplatform.boot.config.security;

/**
 * Wave 3 F-W3-04：Spring Security Principal（Java 21 record）。
 * <p>
 * Controller 可透過 {@code @AuthenticationPrincipal AuthenticatedUser user} 取得當前登入使用者資訊。
 * <p>
 * Wave 3 起所有受保護 endpoint 改用此 record 取代舊版 {@code currentUserId(HttpServletRequest)}。
 */
public record AuthenticatedUser(String userId, String email) {
}
