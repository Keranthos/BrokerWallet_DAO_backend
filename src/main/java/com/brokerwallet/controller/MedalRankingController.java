package com.brokerwallet.controller;

import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 勋章排行榜控制器
 */
@RestController
@RequestMapping("/api/medal")
@CrossOrigin(origins = "*")
public class MedalRankingController {

    private static final Logger logger = LoggerFactory.getLogger(MedalRankingController.class);

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 获取勋章排行榜
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 排行榜数据
     */
    @GetMapping("/ranking")
    @Cacheable(value = "medal-ranking", key = "#page + '-' + #size")
    public ResponseEntity<?> getMedalRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("获取勋章排行榜: page={}, size={}", page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<UserAccount> rankingPage = userAccountService.getMedalRanking(pageable);
            
            // 转换为排行榜格式
            List<Map<String, Object>> rankings = rankingPage.getContent().stream()
                    .map(this::convertToRankingItem)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", rankings);
            response.put("totalElements", rankingPage.getTotalElements());
            response.put("totalPages", rankingPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            logger.info("排行榜查询成功，返回{}条记录", rankings.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取勋章排行榜失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取排行榜失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取用户排名信息
     * @param walletAddress 钱包地址
     * @return 用户排名信息
     */
    @GetMapping("/user-rank/{walletAddress}")
    public ResponseEntity<?> getUserRank(@PathVariable String walletAddress) {
        try {
            logger.info("获取用户排名信息: {}", walletAddress);
            
            UserAccount user = userAccountService.findByWalletAddress(walletAddress);
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            Long rank = userAccountService.getUserRank(user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "walletAddress", user.getWalletAddress(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : "匿名用户",
                "goldMedals", user.getGoldMedals(),
                "silverMedals", user.getSilverMedals(),
                "bronzeMedals", user.getBronzeMedals(),
                "totalMedalScore", user.getTotalMedalScore(),
                "rank", rank,
                "representativeWork", user.canShowRepresentativeWork() ? user.getRepresentativeWork() : null
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取用户排名失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取用户排名失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 获取排行榜统计信息
     */
    @GetMapping("/stats")
    @Cacheable(value = "medal-stats")
    public ResponseEntity<?> getMedalStats() {
        try {
            logger.info("获取勋章统计信息");
            
            Map<String, Object> stats = userAccountService.getMedalStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取勋章统计失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 转换用户账户为排行榜项
     */
    private Map<String, Object> convertToRankingItem(UserAccount user) {
        Map<String, Object> item = new HashMap<>();
        item.put("walletAddress", user.getWalletAddress());
        item.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "匿名用户");
        item.put("goldMedals", user.getGoldMedals());
        item.put("silverMedals", user.getSilverMedals());
        item.put("bronzeMedals", user.getBronzeMedals());
        item.put("totalMedalScore", user.getTotalMedalScore());
        
        // 只有在用户和管理员都同意的情况下才显示代表作
        if (user.canShowRepresentativeWork()) {
            item.put("representativeWork", user.getRepresentativeWork());
        } else {
            item.put("representativeWork", null);
        }
        
        return item;
    }
}


