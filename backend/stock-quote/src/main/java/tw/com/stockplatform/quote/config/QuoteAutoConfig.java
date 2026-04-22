package tw.com.stockplatform.quote.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "tw.com.stockplatform.quote")
@MapperScan(basePackages = "tw.com.stockplatform.quote.repository")
public class QuoteAutoConfig {
}
