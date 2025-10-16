package com.brokerwallet.config;

import com.brokerwallet.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 管理员账户初始化器
 * 在应用启动时自动检查并创建默认的admin账户
 */
@Component
public class AdminInitializer implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);
    
    @Autowired
    private AdminService adminService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("🔐 检查管理员账户...");
        
        try {
            // 确保admin账户存在
            adminService.ensureAdminExists();
            logger.info("✅ 管理员账户检查完成");
        } catch (Exception e) {
            logger.error("❌ 管理员账户初始化失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }
}

