package tw.com.stockplatform.quote.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 純字串 BigDecimal 序列化器（schema-lock §1.3）。
 * <p>
 * 用於 price / previousClose / open / high / low 欄位：
 * <ul>
 *   <li>正值："548.00"（不帶 + 前綴）</li>
 *   <li>負值："-10.00"</li>
 * </ul>
 * 序列化為 JSON string，避免 JS 浮點誤差（前端 Decimal.js 解析）。
 */
public class PlainDecimalSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(value.toPlainString());
    }
}
