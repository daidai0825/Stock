package tw.com.stockplatform.common.constant;

import lombok.Getter;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 業務錯誤碼（依 SRS §9）。
 *
 * <ul>
 *   <li>0 成功</li>
 *   <li>1xxx 參數校驗</li>
 *   <li>2xxx 業務邏輯</li>
 *   <li>3xxx 權限</li>
 *   <li>4xxx 資源</li>
 *   <li>5xxx 第三方服務</li>
 *   <li>9xxx 系統</li>
 * </ul>
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    // 1xxx 參數錯誤
    PARAM_REQUIRED(1001, "必填參數缺失"),
    PARAM_FORMAT_INVALID(1002, "參數格式錯誤"),
    PARAM_OUT_OF_RANGE(1003, "參數值超出範圍"),
    PARAM_TOO_LONG(1004, "參數長度超過上限"),

    // 2xxx 業務邏輯
    EMAIL_ALREADY_REGISTERED(2010, "Email 已被註冊"),
    EMAIL_OR_PASSWORD_INCORRECT(2011, "Email 或密碼錯誤"),
    EMAIL_NOT_VERIFIED(2012, "Email 尚未驗證"),
    VERIFY_TOKEN_EXPIRED(2013, "驗證 Token 已過期"),
    VERIFY_TOKEN_INVALID(2014, "驗證 Token 無效"),
    VERIFY_RESEND_LIMIT(2015, "24 小時內驗證信寄送已達 5 次上限"),
    OLD_PASSWORD_INCORRECT(2016, "舊密碼錯誤"),
    WATCHLIST_ALREADY_EXISTS(2020, "該股票已在自選股清單"),
    WATCHLIST_LIMIT_REACHED(2021, "自選股已達上限 50 檔"),
    WATCHLIST_GROUP_LIMIT(2022, "自訂分組已達上限 10 個"),
    ALERT_DUPLICATE(2030, "已存在相同提醒條件"),
    ALERT_DELETED(2031, "提醒條件已被刪除"),
    ACCOUNT_CLOSED(2040, "該帳號已被註銷"),
    ACCOUNT_CLOSE_EXPIRED(2041, "註銷申請已超過 30 日，無法復原"),

    // 3xxx 權限
    UNAUTHORIZED(3001, "未登入"),
    TOKEN_EXPIRED(3002, "Token 已過期"),
    TOKEN_INVALID(3003, "Token 無效或已被撤銷"),
    FORBIDDEN(3004, "無權限執行此操作"),
    ACCOUNT_SUSPENDED(3010, "帳號已被停權"),
    ACCOUNT_DELETED(3011, "帳號已註銷"),

    // 4xxx 資源
    STOCK_NOT_FOUND(4001, "查無此股票"),
    USER_NOT_FOUND(4002, "查無此使用者"),
    ALERT_NOT_FOUND(4003, "查無此提醒條件"),
    NOTIFY_RECORD_NOT_FOUND(4004, "查無此推播紀錄"),
    STOCK_DELISTED(4010, "該股票已下市"),

    // 5xxx 第三方
    EMAIL_SERVICE_ERROR(5001, "Email 寄送服務異常"),
    WEB_PUSH_SERVICE_ERROR(5002, "Web Push 服務異常"),
    TWSE_DATA_SOURCE_ERROR(5010, "TWSE 資料源暫時無法存取"),
    MOPS_DATA_SOURCE_ERROR(5011, "MOPS 資料源暫時無法存取"),
    CHIP_DATA_SOURCE_ERROR(5012, "籌碼資料源暫時無法存取"),
    OTC_DATA_SOURCE_ERROR(5013, "OTC 資料源暫時無法存取"),
    MOPS_FORMAT_CHANGED(5014, "MOPS API 格式異動，請通知維運團隊"),

    // 9xxx 系統
    SYSTEM_BUSY(9001, "系統繁忙，請稍後再試"),
    SYSTEM_MAINTENANCE(9002, "系統維護中"),
    FEATURE_NOT_AVAILABLE(9003, "功能尚未開放，敬請期待"),
    UNKNOWN_ERROR(9999, "未知錯誤");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 透過 code 反查 enum，使用 Optional 符合規範。
     */
    public static Optional<ErrorCode> fromCode(int code) {
        return Stream.of(values())
            .filter(e -> e.code == code)
            .findFirst();
    }
}
