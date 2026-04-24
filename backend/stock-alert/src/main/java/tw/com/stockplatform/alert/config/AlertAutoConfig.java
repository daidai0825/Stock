package tw.com.stockplatform.alert.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Wave 3 stock-alert 模組自動配置。
 * <p>
 * 業務邏輯（價格警示 CRUD + 觸發引擎）於 W3 W3-W4 開發週期實作。
 * <p>
 * 注意（P3 決策）：@Scheduled 排程 Wave 3 暫保持單 Pod，不引入 ShedLock。
 * 技術債 TD-W3-001 於 Wave 4 評估。
 */
@Configuration
@ComponentScan(basePackages = "tw.com.stockplatform.alert")
@MapperScan(basePackages = "tw.com.stockplatform.alert.repository")
public class AlertAutoConfig {
}
