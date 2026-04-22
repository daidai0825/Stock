package tw.com.stockplatform.fundamental.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "tw.com.stockplatform.fundamental")
@MapperScan(basePackages = "tw.com.stockplatform.fundamental.repository")
public class FundamentalAutoConfig {
}
