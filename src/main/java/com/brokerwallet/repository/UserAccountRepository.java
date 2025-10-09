package com.brokerwallet.repository;

import com.brokerwallet.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户账户数据访问层
 * 提供用户账户的CRUD操作
 */
@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    /**
     * 根据钱包地址查找用户（主键查询）
     */
    Optional<UserAccount> findByWalletAddress(String walletAddress);
    
    /**
     * 根据显示名称查找用户（模糊匹配）
     */
    List<UserAccount> findByDisplayNameContainingIgnoreCase(String displayName);
    
    /**
     * 根据显示名称精确查找用户
     */
    Optional<UserAccount> findByDisplayName(String displayName);
    
    /**
     * 根据账户状态查找用户
     */
    List<UserAccount> findByStatusOrderByCreateTimeDesc(UserAccount.AccountStatus status);
    
    /**
     * 获取勋章排行榜（按加权分数排序）
     */
    @Query("SELECT u FROM UserAccount u WHERE u.status = :status AND (u.goldMedals > 0 OR u.silverMedals > 0 OR u.bronzeMedals > 0) ORDER BY (u.goldMedals * 3 + u.silverMedals * 2 + u.bronzeMedals) DESC, u.goldMedals DESC, u.silverMedals DESC, u.bronzeMedals DESC")
    List<UserAccount> findMedalRanking(@Param("status") UserAccount.AccountStatus status);
    
    /**
     * 获取前N名用户排行榜
     */
    @Query(value = "SELECT * FROM user_accounts WHERE status = :status AND (gold_medals > 0 OR silver_medals > 0 OR bronze_medals > 0) ORDER BY (gold_medals * 3 + silver_medals * 2 + bronze_medals) DESC, gold_medals DESC, silver_medals DESC, bronze_medals DESC LIMIT :limit", nativeQuery = true)
    List<UserAccount> findTopRanking(@Param("status") String status, @Param("limit") int limit);
    
    /**
     * 统计总用户数
     */
    long countByStatus(UserAccount.AccountStatus status);
    
    /**
     * 统计有勋章的用户数
     */
    @Query("SELECT COUNT(u) FROM UserAccount u WHERE u.status = :status AND (u.goldMedals > 0 OR u.silverMedals > 0 OR u.bronzeMedals > 0)")
    long countUsersWithMedals(@Param("status") UserAccount.AccountStatus status);
    
    /**
     * 统计总勋章数
     */
    @Query("SELECT COALESCE(SUM(u.goldMedals + u.silverMedals + u.bronzeMedals), 0) FROM UserAccount u WHERE u.status = :status")
    long getTotalMedals(@Param("status") UserAccount.AccountStatus status);
    
    /**
     * 查找需要同步区块链数据的用户
     */
    @Query("SELECT u FROM UserAccount u WHERE u.status = :status AND (u.goldMedals != u.blockchainGoldMedals OR u.silverMedals != u.blockchainSilverMedals OR u.bronzeMedals != u.blockchainBronzeMedals)")
    List<UserAccount> findUsersNeedingSync(@Param("status") UserAccount.AccountStatus status);
    
    /**
     * 查找待审核代表作展示的用户
     */
    @Query("SELECT u FROM UserAccount u WHERE u.status = :status AND u.showRepresentativeWork = true AND u.adminApprovedDisplay = false AND u.representativeWork IS NOT NULL")
    List<UserAccount> findPendingRepresentativeWorkApproval(@Param("status") UserAccount.AccountStatus status);
    
    /**
     * 查找可以在排行榜展示代表作的用户
     */
    @Query("SELECT u FROM UserAccount u WHERE u.status = :status AND u.showRepresentativeWork = true AND u.adminApprovedDisplay = true AND u.representativeWork IS NOT NULL")
    List<UserAccount> findUsersWithApprovedRepresentativeWork(@Param("status") UserAccount.AccountStatus status);
    
    // ===== 新增优化查询方法 =====
    
    /**
     * 获取勋章排行榜（分页，使用计算列排序，只显示有勋章的用户）
     * 排序规则：
     * 1. 总分（金×3 + 银×2 + 铜×1）降序
     * 2. 金牌数降序
     * 3. 银牌数降序
     * 4. 铜牌数降序（新增，确保完全相同时有稳定排序）
     * 5. 创建时间升序（先注册的用户排前面）
     */
    @Query("SELECT u FROM UserAccount u WHERE (u.goldMedals > 0 OR u.silverMedals > 0 OR u.bronzeMedals > 0) ORDER BY (u.goldMedals * 3 + u.silverMedals * 2 + u.bronzeMedals) DESC, u.goldMedals DESC, u.silverMedals DESC, u.bronzeMedals DESC, u.createTime ASC")
    Page<UserAccount> findAllByOrderByTotalMedalScoreDescGoldMedalsDescSilverMedalsDesc(Pageable pageable);
    
    /**
     * 根据总分数计算排名
     */
    @Query("SELECT COUNT(u) FROM UserAccount u WHERE (u.goldMedals * 3 + u.silverMedals * 2 + u.bronzeMedals) > :totalMedalScore")
    Long countByTotalMedalScoreGreaterThan(@Param("totalMedalScore") Integer totalMedalScore);
    
    /**
     * 获取排名最高的用户
     */
    @Query("SELECT u FROM UserAccount u ORDER BY (u.goldMedals * 3 + u.silverMedals * 2 + u.bronzeMedals) DESC, u.goldMedals DESC, u.silverMedals DESC LIMIT 1")
    UserAccount findTopByOrderByTotalMedalScoreDescGoldMedalsDescSilverMedalsDesc();
    
    /**
     * 统计各种勋章总数
     */
    @Query("SELECT COALESCE(SUM(u.goldMedals), 0) FROM UserAccount u")
    Long sumGoldMedals();
    
    @Query("SELECT COALESCE(SUM(u.silverMedals), 0) FROM UserAccount u")
    Long sumSilverMedals();
    
    @Query("SELECT COALESCE(SUM(u.bronzeMedals), 0) FROM UserAccount u")
    Long sumBronzeMedals();
}
