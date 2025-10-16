package com.brokerwallet.service;

import com.brokerwallet.entity.Admin;
import com.brokerwallet.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 管理员服务类
 * 处理管理员登录、注册等业务逻辑
 */
@Service
public class AdminService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    
    @Autowired
    private AdminRepository adminRepository;
    
    /**
     * 管理员登录验证
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功返回Admin对象，失败返回null
     */
    public Admin login(String username, String password) {
        logger.info("管理员登录尝试: username={}", username);
        
        // 查找管理员
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (!adminOpt.isPresent()) {
            logger.warn("管理员不存在: username={}", username);
            return null;
        }
        
        Admin admin = adminOpt.get();
        
        // 检查账户状态
        if (admin.getStatus() != Admin.AccountStatus.ACTIVE) {
            logger.warn("管理员账户未激活: username={}, status={}", username, admin.getStatus());
            return null;
        }
        
        // 验证密码
        String hashedPassword = hashPassword(password);
        if (!hashedPassword.equals(admin.getPassword())) {
            logger.warn("密码错误: username={}", username);
            return null;
        }
        
        // 更新最后登录时间
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);
        
        logger.info("管理员登录成功: username={}, id={}", username, admin.getId());
        return admin;
    }
    
    /**
     * 创建新管理员（需要已有管理员的验证）
     * @param newUsername 新管理员用户名
     * @param newPassword 新管理员密码
     * @param creatorUsername 创建者用户名
     * @param creatorPassword 创建者密码
     * @return 创建成功返回新Admin对象，失败返回null
     */
    public Admin createAdmin(String newUsername, String newPassword, 
                            String creatorUsername, String creatorPassword) {
        logger.info("尝试创建新管理员: newUsername={}, creatorUsername={}", 
                newUsername, creatorUsername);
        
        // 1. 验证创建者身份
        Admin creator = login(creatorUsername, creatorPassword);
        if (creator == null) {
            logger.warn("创建者验证失败: creatorUsername={}", creatorUsername);
            return null;
        }
        
        // 2. 检查新用户名是否已存在
        if (adminRepository.existsByUsername(newUsername)) {
            logger.warn("用户名已存在: newUsername={}", newUsername);
            return null;
        }
        
        // 3. 检查新用户名是否与创建者用户名相同
        if (newUsername.equals(creatorUsername)) {
            logger.warn("新用户名不能与创建者用户名相同: newUsername={}", newUsername);
            return null;
        }
        
        // 4. 创建新管理员
        Admin newAdmin = new Admin();
        newAdmin.setUsername(newUsername);
        newAdmin.setPassword(hashPassword(newPassword));
        newAdmin.setDisplayName(newUsername);
        newAdmin.setRole("admin");
        newAdmin.setStatus(Admin.AccountStatus.ACTIVE);
        newAdmin.setCreatedBy(creator.getId());
        newAdmin.setCreatedAt(LocalDateTime.now());
        
        Admin savedAdmin = adminRepository.save(newAdmin);
        logger.info("新管理员创建成功: username={}, id={}, createdBy={}", 
                newUsername, savedAdmin.getId(), creator.getId());
        
        return savedAdmin;
    }
    
    /**
     * 创建新管理员（带详细错误信息）
     * @param newUsername 新管理员用户名
     * @param newPassword 新管理员密码
     * @param creatorUsername 创建者用户名
     * @param creatorPassword 创建者密码
     * @return 创建结果，包含成功状态和错误信息
     */
    public CreateAdminResult createAdminWithDetails(String newUsername, String newPassword, 
                                                   String creatorUsername, String creatorPassword) {
        logger.info("尝试创建新管理员: newUsername={}, creatorUsername={}", 
                newUsername, creatorUsername);
        
        // 1. 验证创建者身份
        Admin creator = login(creatorUsername, creatorPassword);
        if (creator == null) {
            logger.warn("创建者验证失败: creatorUsername={}", creatorUsername);
            return new CreateAdminResult(null, "创建者验证失败：用户名或密码错误");
        }
        
        // 2. 检查新用户名是否与创建者用户名相同
        if (newUsername.equals(creatorUsername)) {
            logger.warn("新用户名不能与创建者用户名相同: newUsername={}", newUsername);
            return new CreateAdminResult(null, "新管理员用户名不能与创建者用户名相同");
        }
        
        // 3. 检查新用户名是否已存在
        if (adminRepository.existsByUsername(newUsername)) {
            logger.warn("用户名已存在: newUsername={}", newUsername);
            return new CreateAdminResult(null, "用户名已存在，请选择其他用户名");
        }
        
        // 4. 创建新管理员
        Admin newAdmin = new Admin();
        newAdmin.setUsername(newUsername);
        newAdmin.setPassword(hashPassword(newPassword));
        newAdmin.setDisplayName(newUsername);
        newAdmin.setRole("admin");
        newAdmin.setStatus(Admin.AccountStatus.ACTIVE);
        newAdmin.setCreatedBy(creator.getId());
        newAdmin.setCreatedAt(LocalDateTime.now());
        
        Admin savedAdmin = adminRepository.save(newAdmin);
        logger.info("新管理员创建成功: username={}, id={}, createdBy={}", 
                newUsername, savedAdmin.getId(), creator.getId());
        
        return new CreateAdminResult(savedAdmin, null);
    }
    
    /**
     * 创建管理员结果类
     */
    public static class CreateAdminResult {
        private final Admin admin;
        private final String errorMessage;
        
        public CreateAdminResult(Admin admin, String errorMessage) {
            this.admin = admin;
            this.errorMessage = errorMessage;
        }
        
        public Admin getAdmin() {
            return admin;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean isSuccess() {
            return admin != null && errorMessage == null;
        }
    }
    
    /**
     * 密码哈希（使用SHA-256）
     * 注意：生产环境建议使用BCrypt或其他更安全的哈希算法
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("密码哈希失败", e);
            throw new RuntimeException("密码哈希失败", e);
        }
    }
    
    /**
     * 检查是否存在任何管理员（用于初始化）
     */
    public boolean hasAnyAdmin() {
        return adminRepository.count() > 0;
    }
    
    /**
     * 确保admin账户存在（如果不存在则创建）
     */
    public void ensureAdminExists() {
        if (!adminRepository.existsByUsername("admin")) {
            logger.info("admin账户不存在，正在创建...");
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword("240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9"); // admin123的SHA-256
            admin.setDisplayName("默认管理员");
            admin.setRole("admin");
            admin.setStatus(Admin.AccountStatus.ACTIVE);
            admin.setCreatedAt(LocalDateTime.now());
            
            adminRepository.save(admin);
            logger.info("admin账户创建成功");
        } else {
            logger.info("admin账户已存在");
        }
    }
    
    /**
     * 创建默认管理员（仅在没有任何管理员时使用）
     */
    public Admin createDefaultAdmin(String username, String password) {
        if (hasAnyAdmin()) {
            logger.warn("已存在管理员，无法创建默认管理员");
            return null;
        }
        
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(hashPassword(password));
        admin.setDisplayName("默认管理员");
        admin.setRole("admin");
        admin.setStatus(Admin.AccountStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        
        Admin savedAdmin = adminRepository.save(admin);
        logger.info("默认管理员创建成功: username={}, id={}", username, savedAdmin.getId());
        
        return savedAdmin;
    }
}

