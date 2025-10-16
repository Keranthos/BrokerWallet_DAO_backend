package com.brokerwallet.repository;

import com.brokerwallet.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 管理员数据访问层
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    /**
     * 根据用户名查找管理员
     */
    Optional<Admin> findByUsername(String username);
    
    /**
     * 根据邮箱查找管理员
     */
    Optional<Admin> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
}

