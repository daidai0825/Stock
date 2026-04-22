package tw.com.stockplatform.quote.enums;

/**
 * K 線資料週期（schema-lock v1.0 §3.2）。
 * <p>
 * 取代原 QuoteHistoryRequest.month（LocalDate）語意。
 */
public enum KLinePeriod {

    /** 日 K（預設查近 90 個自然日） */
    daily,

    /** 週 K（預設查近 1 年） */
    weekly,

    /** 月 K（預設查近 5 年） */
    monthly
}
