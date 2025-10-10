package com.brokerwallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 证明文件实体类
 * 用于存储用户上传的证明材料信息
 */
@Entity
@Table(name = "proof_files")
public class ProofFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    
    /**
     * 文件类型/MIME类型
     */
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    /**
     * 文件存储路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    /**
     * 文件MD5哈希值，用于防重复和完整性校验
     */
    @Column(name = "file_hash", length = 32)
    private String fileHash;
    
    /**
     * 上传时间
     */
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
    
    /**
     * 关联用户账户ID（数字外键，性能优化）
     */
    @Column(name = "user_account_id", nullable = false)
    private Long userAccountId;
    
    /**
     * 审核状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", nullable = false)
    private AuditStatus auditStatus = AuditStatus.PENDING;
    
    /**
     * 审核时间
     */
    @Column(name = "audit_time")
    private LocalDateTime auditTime;
    
    /**
     * 发放的勋章类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "medal_awarded", nullable = false)
    private MedalType medalAwarded = MedalType.NONE;
    
    /**
     * 勋章发放时间
     */
    @Column(name = "medal_award_time")
    private LocalDateTime medalAwardTime;
    
    /**
     * 勋章发放的区块链交易哈希
     */
    @Column(name = "medal_transaction_hash", length = 66)
    private String medalTransactionHash;
    
    /**
     * 代币奖励数量（BKC，以Token为单位，不是wei）
     */
    @Column(name = "token_reward")
    private java.math.BigDecimal tokenReward;
    
    /**
     * 代币奖励交易哈希
     */
    @Column(name = "token_reward_tx_hash", length = 66)
    private String tokenRewardTxHash;
    
    /**
     * NFT图片的SHA-256哈希值，用于检查唯一性
     */
    @Column(name = "nft_image_hash", length = 64)
    private String nftImageHash;
    
    /**
     * 提交批次ID，用于标识同一次提交的多个文件
     * 格式：BATCH_{userId}_{timestamp}
     */
    @Column(name = "submission_batch_id", length = 100)
    private String submissionBatchId;
    
    /**
     * 文件状态：ACTIVE-正常, DELETED-已删除
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FileStatus status = FileStatus.ACTIVE;
    
    // 构造方法
    public ProofFile() {
        this.uploadTime = LocalDateTime.now();
    }
    
    public ProofFile(String fileName, String originalName, String fileType, 
                     Long fileSize, String filePath, Long userAccountId) {
        this();
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.userAccountId = userAccountId;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public LocalDateTime getUploadTime() {
        return uploadTime;
    }
    
    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
    
    public Long getUserId() {
        return userAccountId;
    }
    
    public void setUserId(Long userId) {
        this.userAccountId = userId;
    }
    
    public FileStatus getStatus() {
        return status;
    }
    
    public void setStatus(FileStatus status) {
        this.status = status;
    }
    
    public String getRemarks() {
        return ""; // 简化版本暂时返回空字符串
    }
    
    public Long getUserAccountId() {
        return userAccountId;
    }
    
    public void setUserAccountId(Long userAccountId) {
        this.userAccountId = userAccountId;
    }
    
    public AuditStatus getAuditStatus() {
        return auditStatus;
    }
    
    public void setAuditStatus(AuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }
    
    public LocalDateTime getAuditTime() {
        return auditTime;
    }
    
    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }
    
    public MedalType getMedalAwarded() {
        return medalAwarded;
    }
    
    public void setMedalAwarded(MedalType medalAwarded) {
        this.medalAwarded = medalAwarded;
    }
    
    public LocalDateTime getMedalAwardTime() {
        return medalAwardTime;
    }
    
    public void setMedalAwardTime(LocalDateTime medalAwardTime) {
        this.medalAwardTime = medalAwardTime;
    }
    
    public String getMedalTransactionHash() {
        return medalTransactionHash;
    }
    
    public void setMedalTransactionHash(String medalTransactionHash) {
        this.medalTransactionHash = medalTransactionHash;
    }
    
    public java.math.BigDecimal getTokenReward() {
        return tokenReward;
    }
    
    public void setTokenReward(java.math.BigDecimal tokenReward) {
        this.tokenReward = tokenReward;
    }
    
    public String getTokenRewardTxHash() {
        return tokenRewardTxHash;
    }
    
    public void setTokenRewardTxHash(String tokenRewardTxHash) {
        this.tokenRewardTxHash = tokenRewardTxHash;
    }
    
    public String getNftImageHash() {
        return nftImageHash;
    }
    
    public void setNftImageHash(String nftImageHash) {
        this.nftImageHash = nftImageHash;
    }
    
    public String getSubmissionBatchId() {
        return submissionBatchId;
    }
    
    public void setSubmissionBatchId(String submissionBatchId) {
        this.submissionBatchId = submissionBatchId;
    }
    
    @Override
    public String toString() {
        return "ProofFile{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", originalName='" + originalName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileSize=" + fileSize +
                ", userAccountId=" + userAccountId +
                ", auditStatus=" + auditStatus +
                ", medalAwarded=" + medalAwarded +
                ", uploadTime=" + uploadTime +
                ", status=" + status +
                '}';
    }
    
    /**
     * 文件状态枚举
     */
    public enum FileStatus {
        ACTIVE("正常"),
        DELETED("已删除");
        
        private final String description;
        
        FileStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 审核状态枚举
     */
    public enum AuditStatus {
        PENDING("待审核"),
        APPROVED("审核通过"),
        REJECTED("审核拒绝");
        
        private final String description;
        
        AuditStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 勋章类型枚举
     */
    public enum MedalType {
        NONE("无勋章"),
        GOLD("金牌"),
        SILVER("银牌"),
        BRONZE("铜牌");
        
        private final String description;
        
        MedalType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

