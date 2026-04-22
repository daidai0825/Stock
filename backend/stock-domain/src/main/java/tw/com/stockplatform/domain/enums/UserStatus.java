package tw.com.stockplatform.domain.enums;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 會員狀態（對應 USERS.status）。
 */
public enum UserStatus {

    /** 註冊但 Email 未驗證（Wave 1 略過驗證流程，預設直接 ACTIVE） */
    UNVERIFIED,
    ACTIVE,
    SUSPENDED,
    CLOSED;

    public static Optional<UserStatus> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Stream.of(values())
            .filter(s -> s.name().equalsIgnoreCase(name))
            .findFirst();
    }
}
