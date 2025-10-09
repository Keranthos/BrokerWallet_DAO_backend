package com.brokerwallet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置静态资源访问和CORS
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源访问
     * 允许通过URL直接访问uploads目录下的图片
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源映射：/uploads/** -> uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600); // 缓存1小时
        
        System.out.println("✅ Static resource handler configured: /uploads/** -> file:uploads/");
    }

    /**
     * 配置CORS（跨域资源共享）
     * 允许手机端通过IP地址访问后端
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 使用allowedOriginPatterns而不是allowedOrigins
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 允许携带凭证（cookies等）
                .maxAge(3600);
        
        System.out.println("✅ CORS configured: Allow all origin patterns with credentials");
    }
}

