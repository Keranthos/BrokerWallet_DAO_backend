package com.brokerwallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户账户实体类
 * 以钱包地址为主键的去中心化用户管理
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "wallet_address", length = 42, nullable = false, unique = true)
    private String walletAddress;
    
    /**
     * 用户花名（用户在钱包前端填写，可选）
     */
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    /**
     * 代表作描述（用户在钱包前端填写，可选）
     */
    @Column(name = "representative_work", length = 500)
    private String representativeWork;
    
    /**
     * 用户选择：是否在排行榜展示代表作
     */
    @Column(name = "show_representative_work", nullable = false)
    private Boolean showRepresentativeWork = false;
    
    /**
     * 管理员是否同意展示代表作
     */
    @Column(name = "admin_approved_display", nullable = false)
    private Boolean adminApprovedDisplay = false;
    
    /**
     * 金牌数量
     */
    @Column(name = "gold_medals", nullable = false)
    private Integer goldMedals = 0;
    
    /**
     * 银牌数量
     */
    @Column(name = "silver_medals", nullable = false)
    private Integer silverMedals = 0;
    
    /**
     * 铜牌数量
     */
    @Column(name = "bronze_medals", nullable = false)
    private Integer bronzeMedals = 0;
    
    /**
     * 总证明文件数
     */
    @Column(name = "total_proofs", nullable = false)
    private Integer totalProofs = 0;
    
    /**
     * 总NFT数量
     */
    @Column(name = "total_nfts", nullable = false)
    private Integer totalNfts = 0;
    
    /**
     * 已审核通过的证明数
     */
    @Column(name = "verified_proofs", nullable = false)
    private Integer verifiedProofs = 0;
    
    /**
     * 最后一次与区块链同步时间
     */
    @Column(name = "blockchain_sync_time")
    private LocalDateTime blockchainSyncTime;
    
    /**
     * 区块链上的金牌数量（用于一致性检查）
     */
    @Column(name = "blockchain_gold_medals", nullable = false)
    private Integer blockchainGoldMedals = 0;
    
    /**
     * 区块链上的银牌数量（用于一致性检查）
     */
    @Column(name = "blockchain_silver_medals", nullable = false)
    private Integer blockchainSilverMedals = 0;
    
    /**
     * 区块链上的铜牌数量（用于一致性检查）
     */
    @Column(name = "blockchain_bronze_medals", nullable = false)
    private Integer blockchainBronzeMedals = 0;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    /**
     * 账户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;
    
    // 构造方法
    public UserAccount() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    public UserAccount(String walletAddress) {
        this();
        this.walletAddress = walletAddress;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getWalletAddress() {
        return walletAddress;
    }
    
    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getRepresentativeWork() {
        return representativeWork;
    }
    
    public void setRepresentativeWork(String representativeWork) {
        this.representativeWork = representativeWork;
    }
    
    public Boolean getShowRepresentativeWork() {
        return showRepresentativeWork;
    }
    
    public void setShowRepresentativeWork(Boolean showRepresentativeWork) {
        this.showRepresentativeWork = showRepresentativeWork;
    }
    
    public Boolean getAdminApprovedDisplay() {
        return adminApprovedDisplay;
    }
    
    public void setAdminApprovedDisplay(Boolean adminApprovedDisplay) {
        this.adminApprovedDisplay = adminApprovedDisplay;
    }
    
    public Integer getGoldMedals() {
        return goldMedals;
    }
    
    public void setGoldMedals(Integer goldMedals) {
        this.goldMedals = goldMedals;
    }
    
    public Integer getSilverMedals() {
        return silverMedals;
    }
    
    public void setSilverMedals(Integer silverMedals) {
        this.silverMedals = silverMedals;
    }
    
    public Integer getBronzeMedals() {
        return bronzeMedals;
    }
    
    public void setBronzeMedals(Integer bronzeMedals) {
        this.bronzeMedals = bronzeMedals;
    }
    
    public Integer getTotalProofs() {
        return totalProofs;
    }
    
    public void setTotalProofs(Integer totalProofs) {
        this.totalProofs = totalProofs;
    }
    
    public Integer getTotalNfts() {
        return totalNfts;
    }
    
    public void setTotalNfts(Integer totalNfts) {
        this.totalNfts = totalNfts;
    }
    
    public Integer getVerifiedProofs() {
        return verifiedProofs;
    }
    
    public void setVerifiedProofs(Integer verifiedProofs) {
        this.verifiedProofs = verifiedProofs;
    }
    
    public LocalDateTime getBlockchainSyncTime() {
        return blockchainSyncTime;
    }
    
    public void setBlockchainSyncTime(LocalDateTime blockchainSyncTime) {
        this.blockchainSyncTime = blockchainSyncTime;
    }
    
    public Integer getBlockchainGoldMedals() {
        return blockchainGoldMedals;
    }
    
    public void setBlockchainGoldMedals(Integer blockchainGoldMedals) {
        this.blockchainGoldMedals = blockchainGoldMedals;
    }
    
    public Integer getBlockchainSilverMedals() {
        return blockchainSilverMedals;
    }
    
    public void setBlockchainSilverMedals(Integer blockchainSilverMedals) {
        this.blockchainSilverMedals = blockchainSilverMedals;
    }
    
    public Integer getBlockchainBronzeMedals() {
        return blockchainBronzeMedals;
    }
    
    public void setBlockchainBronzeMedals(Integer blockchainBronzeMedals) {
        this.blockchainBronzeMedals = blockchainBronzeMedals;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public AccountStatus getStatus() {
        return status;
    }
    
    public void setStatus(AccountStatus status) {
        this.status = status;
    }
    
    /**
     * 计算总勋章数（加权）
     * 金牌=3分，银牌=2分，铜牌=1分
     */
    public int getTotalMedalScore() {
        return (goldMedals != null ? goldMedals * 3 : 0) + 
               (silverMedals != null ? silverMedals * 2 : 0) + 
               (bronzeMedals != null ? bronzeMedals : 0);
    }
    
    /**
     * 检查是否可以在排行榜展示代表作
     */
    public boolean canShowRepresentativeWork() {
        return showRepresentativeWork && adminApprovedDisplay && representativeWork != null && !representativeWork.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "UserAccount{" +
                "walletAddress='" + walletAddress + '\'' +
                ", displayName='" + displayName + '\'' +
                ", goldMedals=" + goldMedals +
                ", silverMedals=" + silverMedals +
                ", bronzeMedals=" + bronzeMedals +
                ", showRepresentativeWork=" + showRepresentativeWork +
                ", adminApprovedDisplay=" + adminApprovedDisplay +
                '}';
    }
    
    /**
     * 账户状态枚举
     */
    public enum AccountStatus {
        ACTIVE("正常"),
        INACTIVE("未激活"),
        BANNED("已封禁");
        
        private final String description;
        
        AccountStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
