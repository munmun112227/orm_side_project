package com.pos.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * POS 雙 ORM 報表與效能分析系統應用程式入口。
 */
@SpringBootApplication
@MapperScan("com.pos.analytics.mapper")
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
