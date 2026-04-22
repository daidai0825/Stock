package tw.com.stockplatform.member.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "tw.com.stockplatform.member")
@MapperScan(basePackages = "tw.com.stockplatform.member.repository")
public class MemberAutoConfig {
}
