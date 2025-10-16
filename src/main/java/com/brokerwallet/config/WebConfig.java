package com.brokerwallet.config;

import com.brokerwallet.interceptor.AdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置跨域和拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许所有来源（手机端需要通过IP访问）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // 重要：允许携带Cookie和Session（Web管理员端需要）
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                // 拦截所有管理员相关的接口
                .addPathPatterns(
                        "/api/admin/**",                           // 所有管理员接口
                        "/api/blockchain/nft/mint",                // NFT铸造（仅管理员）
                        "/api/blockchain/check-nft-permission",    // 检查NFT权限（管理员）
                        "/api/blockchain/nft/check-permission",    // 检查铸造权限（管理员）
                        "/api/blockchain/nft/mint-fee",            // 铸造费用查询（管理员）
                        "/api/blockchain/test-contract"            // 合约测试（管理员）
                )
                // 排除不需要认证的路径（登录、注册、初始化相关、手机端公开接口）
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/check-init",
                        "/api/auth/ensure-admin",
                        "/api/admin/nft-image/**",      // NFT图片访问（手机端需要）
                        "/api/admin/nft-thumbnail/**",  // NFT缩略图访问（手机端需要）
                        "/api/admin/user/info/**"       // 用户信息查询（手机端需要）
                );
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置uploads目录为静态资源，供手机端直接访问NFT图片
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600); // 缓存1小时
        
        System.out.println("✅ 静态资源配置完成: /uploads/** -> file:uploads/");
    }
}
