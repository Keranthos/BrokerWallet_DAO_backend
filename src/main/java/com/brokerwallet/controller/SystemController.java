package com.brokerwallet.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统信息控制器
 * 提供系统健康检查、版本信息等API
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SystemController {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);
    
    /**
     * 系统健康检查
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "UP");
            response.put("message", "BrokerWallet后端服务运行正常");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            response.put("version", "1.0.0");
            response.put("service", "BrokerWallet Backend");
            
            // 系统信息
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("java.version", System.getProperty("java.version"));
            systemInfo.put("os.name", System.getProperty("os.name"));
            systemInfo.put("os.version", System.getProperty("os.version"));
            systemInfo.put("server.address", InetAddress.getLocalHost().getHostAddress());
            systemInfo.put("server.port", "5000");
            
            response.put("system", systemInfo);
            
            // API端点信息
            Map<String, Object> apiInfo = new HashMap<>();
            apiInfo.put("proof_upload", "/api/proof/upload");
            apiInfo.put("proof_list", "/api/proof/list");
            apiInfo.put("nft_upload", "/api/nft/upload");
            apiInfo.put("nft_mint", "/api/nft/mint");
            apiInfo.put("nft_list", "/api/nft/list");
            
            response.put("api_endpoints", apiInfo);
            
            logger.info("系统健康检查: 服务正常运行");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("系统健康检查异常", e);
            response.put("status", "DOWN");
            response.put("message", "系统异常: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取服务器信息
     * GET /api/server/info
     */
    @GetMapping("/server/info")
    public ResponseEntity<Map<String, Object>> getServerInfo() {
        logger.info("获取服务器信息请求");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 基本信息
            response.put("service_name", "BrokerWallet Backend");
            response.put("version", "1.0.0");
            response.put("description", "BrokerWallet勋章系统后端服务");
            response.put("startup_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 服务器配置
            Map<String, Object> serverConfig = new HashMap<>();
            serverConfig.put("host", InetAddress.getLocalHost().getHostAddress());
            serverConfig.put("port", 5000);
            serverConfig.put("protocol", "HTTP");
            serverConfig.put("max_file_size", "50MB");
            
            response.put("server_config", serverConfig);
            
            // 支持的功能
            Map<String, Object> features = new HashMap<>();
            features.put("proof_file_upload", "支持证明材料上传");
            features.put("nft_image_upload", "支持NFT图片上传");
            features.put("nft_minting", "支持NFT铸造（模拟）");
            features.put("file_management", "支持文件管理和查看");
            features.put("database_storage", "支持MySQL数据库存储");
            
            response.put("features", features);
            
            // 使用说明
            Map<String, Object> usage = new HashMap<>();
            usage.put("android_config", "在Android应用的ServerConfig.java中将SERVER_HOST设置为: " + InetAddress.getLocalHost().getHostAddress());
            usage.put("connection_test", "访问 http://" + InetAddress.getLocalHost().getHostAddress() + ":5000/api/health 测试连接");
            usage.put("proof_upload_endpoint", "POST /api/proof/upload");
            usage.put("nft_upload_endpoint", "POST /api/nft/upload");
            usage.put("nft_mint_endpoint", "POST /api/nft/mint");
            
            response.put("usage", usage);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取服务器信息异常", e);
            response.put("error", "获取服务器信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 测试连接
     * GET /api/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        logger.info("测试连接请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "连接测试成功！");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("server", "BrokerWallet Backend");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取API文档
     * GET /api/docs
     */
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        logger.info("获取API文档请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("title", "BrokerWallet Backend API 文档");
        response.put("version", "1.0.0");
        response.put("description", "BrokerWallet勋章系统后端API接口文档");
        
        // API端点列表
        Map<String, Object> endpoints = new HashMap<>();
        
        // 系统相关
        endpoints.put("GET /api/health", "系统健康检查");
        endpoints.put("GET /api/server/info", "获取服务器信息");
        endpoints.put("GET /api/test", "测试连接");
        
        // 证明文件相关
        endpoints.put("POST /api/proof/upload", "上传证明文件");
        endpoints.put("GET /api/proof/list", "获取证明文件列表");
        endpoints.put("GET /api/proof/detail/{id}", "获取证明文件详情");
        endpoints.put("GET /api/proof/download/{id}", "下载证明文件");
        endpoints.put("DELETE /api/proof/delete/{id}", "删除证明文件");
        endpoints.put("GET /api/proof/search", "搜索证明文件");
        endpoints.put("GET /api/proof/statistics", "获取文件统计信息");
        
        // NFT相关
        endpoints.put("POST /api/nft/upload", "上传NFT图片");
        endpoints.put("POST /api/nft/mint", "NFT铸造");
        endpoints.put("GET /api/nft/list", "获取NFT图片列表");
        endpoints.put("GET /api/nft/detail/{id}", "获取NFT图片详情");
        endpoints.put("GET /api/nft/view/{id}", "查看NFT图片");
        endpoints.put("GET /api/nft/statistics", "获取NFT统计信息");
        
        response.put("endpoints", endpoints);
        
        // 使用示例
        Map<String, Object> examples = new HashMap<>();
        examples.put("upload_proof", "curl -X POST -F 'file=@证明文件.pdf' -F 'userId=user123' http://localhost:5000/api/proof/upload");
        examples.put("upload_nft", "curl -X POST -F 'file=@图片.jpg' -F 'nftName=我的NFT' -F 'userId=user123' http://localhost:5000/api/nft/upload");
        examples.put("mint_nft", "curl -X POST -d 'imageId=1&shares=100' http://localhost:5000/api/nft/mint");
        
        response.put("examples", examples);
        
        return ResponseEntity.ok(response);
    }
}

