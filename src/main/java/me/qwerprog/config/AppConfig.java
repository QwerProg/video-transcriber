package me.qwerprog.aivideotranscriber.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

/**
 * 应用配置类.
 */
@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${cors.allowed-origins:*}") // 从配置文件读取允许的来源
    private String allowedOrigins;

    /**
     * 配置 CORS 跨域访问.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        log.info("Configuring CORS with allowed origins: {}", allowedOrigins);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // 只对 /api/** 路径生效
                        .allowedOrigins(allowedOrigins.split(",")) // 支持多个来源
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false); // 通常 API 不需要凭证
            }
        };
    }

    /**
     * 配置异步任务执行器.
     * 使用 application.properties 中的配置.
     * @return 异步执行器 Bean
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 配置参数将由 Spring Boot 根据 application.properties 自动注入
        // executor.setCorePoolSize(4);
        // executor.setMaxPoolSize(10);
        // executor.setQueueCapacity(100);
        // executor.setThreadNamePrefix("async-task-");
        executor.initialize();
        log.info("Configured ThreadPoolTaskExecutor for async tasks.");
        return executor;
    }

}
