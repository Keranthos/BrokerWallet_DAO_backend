package com.brokerwallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 管理员实体类
 * 用于管理员登录和权限管理
 */
@Entity
@Table(name = "admins")
public class Admin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 管理员用户名（唯一）
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 密码（加密存储）
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    /**
     * 显示名称
     */
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    private String email;
    
    /**
     * 角色（admin, super_admin等）
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role = "admin";
    
    /**
     * 账户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * 创建者ID（用于追溯谁创建了这个管理员）
     */
    @Column(name = "created_by")
    private Long createdBy;
    
    // 构造方法
    public Admin() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Admin(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    @Override
    public String toString() {
        return "Admin{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", role='" + role + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
    
    /**
     * 账户状态枚举
     */
    public enum AccountStatus {
        ACTIVE("激活"),
        DISABLED("禁用"),
        LOCKED("锁定");
        
        private final String description;
        
        AccountStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

