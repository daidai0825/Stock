package tw.com.stockplatform.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EnableRetry
@ComponentScan(basePackages = "tw.com.stockplatform.infrastructure")
@MapperScan(basePackages = "tw.com.stockplatform.infrastructure.mapper")
public class InfrastructureAutoConfig {
}
