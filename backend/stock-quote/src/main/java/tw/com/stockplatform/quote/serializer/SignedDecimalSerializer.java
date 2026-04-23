package tw.com.stockplatform.quote.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 正值帶 "+" 前綴的 BigDecimal 序列化器（schema-lock §1.3）。
 * <p>
 * 用於 change / changePercent 欄位：
 * <ul>
 *   <li>正值："+19.00"</li>
 *   <li>負值："-5.50"（保留負號，不加 +）</li>
 *   <li>零值："+0.00"（視為正值帶 + 前綴）</li>
 * </ul>
 * 序列化為 JSON string，避免 JS 浮點誤差。
 */
public class SignedDecimalSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String plain = value.toPlainString();
        // 負值已有負號，正值與零值補 "+"
        String result = value.signum() < 0 ? plain : "+" + plain;
        gen.writeString(result);
    }
}
