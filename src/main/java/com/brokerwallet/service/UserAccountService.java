package com.brokerwallet.service;

import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户账户服务
 */
@Service
public class UserAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserAccountService.class);
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    /**
     * 根据钱包地址获取或创建用户
     */
    public UserAccount getOrCreateUser(String walletAddress) {
        Optional<UserAccount> existingUser = userAccountRepository.findByWalletAddress(walletAddress);
        
        if (existingUser.isPresent()) {
            logger.info("Found existing user: {}", walletAddress);
            return existingUser.get();
        } else {
            logger.info("Creating new user: {}", walletAddress);
            UserAccount newUser = new UserAccount(walletAddress);
            return userAccountRepository.save(newUser);
        }
    }
    
    /**
     * 保存用户
     */
    public UserAccount save(UserAccount user) {
        user.setUpdateTime(LocalDateTime.now());
        return userAccountRepository.save(user);
    }
    
    /**
     * 根据钱包地址查找用户（Optional版本）
     */
    public Optional<UserAccount> findByWalletAddressOptional(String walletAddress) {
        return userAccountRepository.findByWalletAddress(walletAddress);
    }
    
    /**
     * 根据钱包地址查找用户（直接返回实体）
     */
    public UserAccount findByWalletAddress(String walletAddress) {
        return userAccountRepository.findByWalletAddress(walletAddress).orElse(null);
    }
    
    /**
     * 根据ID查找用户
     */
    public Optional<UserAccount> findById(Long id) {
        return userAccountRepository.findById(id);
    }
    
    /**
     * 获取勋章排行榜（带缓存）
     */
    @Cacheable(value = "medal-ranking", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<UserAccount> getMedalRanking(Pageable pageable) {
        logger.info("查询勋章排行榜: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return userAccountRepository.findAllByOrderByTotalMedalScoreDescGoldMedalsDescSilverMedalsDesc(pageable);
    }
    
    /**
     * 获取用户排名
     */
    public Long getUserRank(Long userId) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }
        
        // 计算比该用户分数高的用户数量 + 1
        Long rank = userAccountRepository.countByTotalMedalScoreGreaterThan(user.getTotalMedalScore()) + 1;
        logger.info("用户{}的排名: {}", userId, rank);
        return rank;
    }
    
    /**
     * 获取勋章统计信息（带缓存）
     */
    @Cacheable(value = "medal-stats")
    public Map<String, Object> getMedalStats() {
        logger.info("获取勋章统计信息");
        
        Map<String, Object> stats = new HashMap<>();
        
        // 总用户数
        long totalUsers = userAccountRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // 有勋章的用户数
        long usersWithMedals = userAccountRepository.countByTotalMedalScoreGreaterThan(0);
        stats.put("usersWithMedals", usersWithMedals);
        
        // 各种勋章总数
        Long totalGoldMedals = userAccountRepository.sumGoldMedals();
        Long totalSilverMedals = userAccountRepository.sumSilverMedals();
        Long totalBronzeMedals = userAccountRepository.sumBronzeMedals();
        
        stats.put("totalGoldMedals", totalGoldMedals != null ? totalGoldMedals : 0);
        stats.put("totalSilverMedals", totalSilverMedals != null ? totalSilverMedals : 0);
        stats.put("totalBronzeMedals", totalBronzeMedals != null ? totalBronzeMedals : 0);
        
        // 最高分数
        UserAccount topUser = userAccountRepository.findTopByOrderByTotalMedalScoreDescGoldMedalsDescSilverMedalsDesc();
        if (topUser != null) {
            stats.put("highestScore", topUser.getTotalMedalScore());
            stats.put("topUserDisplayName", topUser.getDisplayName() != null ? topUser.getDisplayName() : "匿名用户");
        } else {
            stats.put("highestScore", 0);
            stats.put("topUserDisplayName", null);
        }
        
        return stats;
    }
    
    /**
     * 更新用户个人信息
     */
    public void updateUserProfile(Long userId, String displayName, String representativeWork, boolean showRepresentativeWork) {
        Optional<UserAccount> userOpt = userAccountRepository.findById(userId);
        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            
            if (displayName != null && !displayName.trim().isEmpty()) {
                user.setDisplayName(displayName.trim());
            }
            
            if (representativeWork != null && !representativeWork.trim().isEmpty()) {
                user.setRepresentativeWork(representativeWork.trim());
            }
            
            user.setShowRepresentativeWork(showRepresentativeWork);
            user.setUpdateTime(LocalDateTime.now());
            
            userAccountRepository.save(user);
            logger.info("User {} profile updated successfully", userId);
        }
    }
    
}