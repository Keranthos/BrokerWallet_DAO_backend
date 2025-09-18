package com.brokerwallet.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 快速测试控制器
 * 用于测试USB连接和基本功能
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class QuickTestController {
    
    /**
     * 简单的连接测试
     */
    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "USB连接测试成功！");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("server", "BrokerWallet Backend");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 文件上传测试
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> testFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("walletAddress") String walletAddress) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("success", true);
            response.put("message", "文件上传测试成功！");
            response.put("data", Map.of(
                    "fileName", file.getOriginalFilename(),
                    "fileSize", file.getSize(),
                    "fileType", file.getContentType(),
                    "walletAddress", walletAddress,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "文件上传测试失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
