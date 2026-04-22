package tw.com.stockplatform.chip.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "tw.com.stockplatform.chip")
@MapperScan(basePackages = "tw.com.stockplatform.chip.repository")
public class ChipAutoConfig {
}
