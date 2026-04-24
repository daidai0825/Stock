package tw.com.stockplatform.search.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Wave 3 stock-search 模組自動配置。
 * <p>
 * 由 stock-boot 的 @ComponentScan("tw.com.stockplatform") 自動掃描，
 * 此處補充 MyBatis MapperScan 以確保 repository 套件正確掃描。
 */
@Configuration
@ComponentScan(basePackages = "tw.com.stockplatform.search")
@MapperScan(basePackages = "tw.com.stockplatform.search.repository")
public class SearchAutoConfig {
}
