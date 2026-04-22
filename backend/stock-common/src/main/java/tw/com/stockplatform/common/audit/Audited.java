package tw.com.stockplatform.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 標記方法需寫 audit log。
 *
 * <pre>
 * @Audited(action = "MEMBER_REGISTER", target = "USER")
 * public MemberDTO register(...) { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /**
     * 動作代號，例如 MEMBER_REGISTER、MEMBER_LOGIN、PROFILE_UPDATE。
     */
    String action();

    /**
     * 目標資源類型，例如 USER、WATCHLIST、ALERT。
     */
    String target() default "";
}
