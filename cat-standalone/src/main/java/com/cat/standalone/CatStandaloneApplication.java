package com.cat.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cat Agent Platform - 独立启动入口
 *
 * 使用本地JSON文件存储，无需外部数据库依赖
 *
 * 特点：
 * - 零外部依赖，一键启动
 * - JSON文件持久化存储
 * - 简化认证，快速开发
 */
@EnableScheduling
@SpringBootApplication(
    scanBasePackages = {"com.cat.standalone", "com.cat.cliagent"},
    exclude = { DataSourceAutoConfiguration.class }
)
public class CatStandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatStandaloneApplication.class, args);
        System.out.println("\n" +
            "  ═════════════════════════════════════════\n" +
            "  🐱 猫猫多Agent协同系统 启动成功！\n" +
            "  ═════════════════════════════════════════\n" +
            "  API地址: http://localhost:8080/api/v1\n" +
            "  默认用户: admin / admin123\n" +
            "  数据目录: ./data/\n" +
            "  ═════════════════════════════════════════\n"
        );
    }
}