package com.brokerwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling  // 启用定时任务支持
public class BrokerWalletBackendApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting BrokerWallet Backend Service...");
        System.out.println("📱 Supported Features:");
        System.out.println("   - Proof File Upload");
        System.out.println("   - NFT Photo Minting");
        System.out.println("   - Medal Ranking");
        System.out.println("   - Blockchain Integration");
        System.out.println("   - Medal Query & Distribution");
        System.out.println("   - Data Sync Mechanism");
        System.out.println("   - File Management");
        System.out.println("   - Database Storage");
        System.out.println("   - Cache System");
        System.out.println("   - Async Processing");
        System.out.println("   - Scheduled Tasks");
        
        SpringApplication.run(BrokerWalletBackendApplication.class, args);
        
        System.out.println("✅ BrokerWallet Backend Service Started Successfully!");
        System.out.println("🌐 Service URL: http://localhost:5000");
        System.out.println("📚 API Documentation: http://localhost:5000/api/health");
    }
}

