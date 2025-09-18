package com.brokerwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * BrokerWallet后端应用程序启动类
 * 用于处理勋章系统的文件上传和NFT铸造功能
 * 
 * @author BrokerWallet Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching  // 启用缓存支持
@EnableAsync    // 启用异步支持
public class BrokerWalletBackendApplication {

    public static void main(String[] args) {
        System.out.println("🚀 正在启动BrokerWallet后端服务...");
        System.out.println("📱 支持功能：");
        System.out.println("   - 证明材料上传");
        System.out.println("   - NFT照片铸造");
        System.out.println("   - 勋章排行榜");
        System.out.println("   - 文件管理");
        System.out.println("   - 数据库存储");
        System.out.println("   - 缓存系统");
        System.out.println("   - 异步处理");
        
        SpringApplication.run(BrokerWalletBackendApplication.class, args);
        
        System.out.println("✅ BrokerWallet后端服务启动成功！");
        System.out.println("🌐 服务地址: http://localhost:5000");
        System.out.println("📚 API文档: http://localhost:5000/api/health");
    }
}

