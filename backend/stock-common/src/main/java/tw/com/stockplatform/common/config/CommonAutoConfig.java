package tw.com.stockplatform.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * stock-common 自動掃描設定（global handler、TraceIdFilter、AuditAspect）。
 * 由 stock-boot 透過 component scan 載入。
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "tw.com.stockplatform.common")
public class CommonAutoConfig {
}
