package com.brokerwallet.controller;

import com.brokerwallet.entity.Admin;
import com.brokerwallet.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证控制器
 * 处理管理员登录和注册
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AdminService adminService;
    
    private static final String ADMIN_SESSION_KEY = "ADMIN_USER";
    
    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            logger.info("登录请求: username={}", username);
            
            if (username == null || username.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "用户名不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            if (password == null || password.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "密码不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            // 验证登录
            Admin admin = adminService.login(username, password);
            
            if (admin == null) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "用户名或密码错误");
                return ResponseEntity.status(401).body(response);
            }
            
            // 将管理员信息存入Session
            Map<String, Object> adminInfo = Map.of(
                "id", admin.getId(),
                "username", admin.getUsername(),
                "displayName", admin.getDisplayName() != null ? admin.getDisplayName() : admin.getUsername(),
                "role", admin.getRole()
            );
            session.setAttribute(ADMIN_SESSION_KEY, adminInfo);
            
            // 设置Session超时时间（30分钟）
            session.setMaxInactiveInterval(30 * 60);
            
            // 返回成功信息
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user", adminInfo);
            
            logger.info("登录成功: username={}, id={}, sessionId={}", username, admin.getId(), session.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("登录失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "登录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 创建新管理员（需要已有管理员验证）
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String newUsername = request.get("newUsername");
            String newPassword = request.get("newPassword");
            String creatorUsername = request.get("creatorUsername");
            String creatorPassword = request.get("creatorPassword");
            
            logger.info("注册请求: newUsername={}, creatorUsername={}", newUsername, creatorUsername);
            
            // 验证参数
            if (newUsername == null || newUsername.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "新用户名不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "新密码不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            if (creatorUsername == null || creatorUsername.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "创建者用户名不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            if (creatorPassword == null || creatorPassword.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "创建者密码不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            // 创建新管理员（使用详细错误信息）
            AdminService.CreateAdminResult result = adminService.createAdminWithDetails(
                    newUsername, newPassword, creatorUsername, creatorPassword);
            
            if (!result.isSuccess()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", result.getErrorMessage());
                return ResponseEntity.status(400).body(response);
            }
            
            Admin newAdmin = result.getAdmin();
            
            // 返回成功信息
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "管理员创建成功");
            response.put("admin", Map.of(
                "id", newAdmin.getId(),
                "username", newAdmin.getUsername(),
                "displayName", newAdmin.getDisplayName(),
                "role", newAdmin.getRole()
            ));
            
            logger.info("管理员创建成功: username={}, id={}", newUsername, newAdmin.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("注册失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "注册失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 初始化默认管理员（仅在没有任何管理员时可用）
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, Object>> initDefaultAdmin(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查是否已有管理员
            if (adminService.hasAnyAdmin()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "已存在管理员，无法初始化");
                return ResponseEntity.status(400).body(response);
            }
            
            String username = request.get("username");
            String password = request.get("password");
            
            if (username == null || username.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "用户名不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            if (password == null || password.trim().isEmpty()) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "密码不能为空");
                return ResponseEntity.status(400).body(response);
            }
            
            // 创建默认管理员
            Admin admin = adminService.createDefaultAdmin(username, password);
            
            if (admin == null) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "初始化失败");
                return ResponseEntity.status(500).body(response);
            }
            
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "默认管理员创建成功");
            response.put("admin", Map.of(
                "id", admin.getId(),
                "username", admin.getUsername()
            ));
            
            logger.info("默认管理员初始化成功: username={}", username);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("初始化失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "初始化失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 检查是否需要初始化（是否存在管理员）
     */
    @GetMapping("/check-init")
    public ResponseEntity<Map<String, Object>> checkInit() {
        Map<String, Object> response = new HashMap<>();
        
        boolean hasAdmin = adminService.hasAnyAdmin();
        
        response.put("success", true);
        response.put("hasAdmin", hasAdmin);
        response.put("needInit", !hasAdmin);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 确保admin账户存在
     */
    @PostMapping("/ensure-admin")
    public ResponseEntity<Map<String, Object>> ensureAdmin() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            adminService.ensureAdminExists();
            
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "admin账户已确保存在");
            
            logger.info("admin账户确保存在成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("确保admin账户失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "确保admin账户失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 清除Session
            session.invalidate();
            
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "登出成功");
            
            logger.info("登出成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("登出失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "登出失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取当前登录的管理员信息
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentAdmin(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Object adminInfo = session.getAttribute(ADMIN_SESSION_KEY);
            
            if (adminInfo == null) {
                response.put("code", 0);
                response.put("success", false);
                response.put("message", "未登录");
                return ResponseEntity.status(401).body(response);
            }
            
            response.put("code", 1);
            response.put("success", true);
            response.put("user", adminInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取当前管理员信息失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "获取信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

