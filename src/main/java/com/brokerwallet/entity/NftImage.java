package com.brokerwallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * NFT图片实体类
 * 用于存储NFT铸造相关的图片信息
 */
@Entity
@Table(name = "nft_images")
public class NftImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 图片文件名
     */
    @Column(name = "image_name", nullable = false, length = 255)
    private String imageName;
    
    /**
     * 原始图片名
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    
    /**
     * NFT名称
     */
    @Column(name = "nft_name", length = 255)
    private String nftName;
    
    /**
     * NFT描述
     */
    @Column(name = "nft_description", length = 1000)
    private String nftDescription;
    
    /**
     * 图片类型
     */
    @Column(name = "image_type", nullable = false, length = 50)
    private String imageType;
    
    /**
     * 图片大小（字节）
     */
    @Column(name = "image_size", nullable = false)
    private Long imageSize;
    
    /**
     * 图片存储路径
     */
    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;
    
    /**
     * 缩略图路径
     */
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;
    
    /**
     * 图片宽度
     */
    @Column(name = "image_width")
    private Integer imageWidth;
    
    /**
     * 图片高度
     */
    @Column(name = "image_height")
    private Integer imageHeight;
    
    /**
     * Base64编码的图片数据（用于NFT铸造）
     */
    @Column(name = "base64_data", columnDefinition = "LONGTEXT")
    private String base64Data;
    
    /**
     * 图片哈希值
     */
    @Column(name = "image_hash", length = 32)
    private String imageHash;
    
    /**
     * 上传时间
     */
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;
    
    /**
     * 关联用户账户ID（数字外键）
     */
    @Column(name = "user_account_id", nullable = false)
    private Long userAccountId;
    
    /**
     * 关联证明文件ID（强关联）
     */
    @Column(name = "proof_file_id", nullable = false)
    private Long proofFileId;
    
    /**
     * NFT铸造状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mint_status", nullable = false)
    private MintStatus mintStatus = MintStatus.NOT_STARTED;
    
    /**
     * 区块链交易哈希（铸造成功后记录）
     */
    @Column(name = "transaction_hash", length = 66)
    private String transactionHash;
    
    /**
     * NFT Token ID（铸造成功后记录）
     */
    @Column(name = "token_id", length = 100)
    private String tokenId;
    
    /**
     * 图片状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ImageStatus status = ImageStatus.ACTIVE;
    
    // 构造方法
    public NftImage() {
        this.uploadTime = LocalDateTime.now();
    }
    
    public NftImage(String imageName, String originalName, String imageType, 
                    Long imageSize, String imagePath) {
        this();
        this.imageName = imageName;
        this.originalName = originalName;
        this.imageType = imageType;
        this.imageSize = imageSize;
        this.imagePath = imagePath;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getImageName() {
        return imageName;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public String getNftName() {
        return nftName;
    }
    
    public void setNftName(String nftName) {
        this.nftName = nftName;
    }
    
    public String getNftDescription() {
        return nftDescription;
    }
    
    public void setNftDescription(String nftDescription) {
        this.nftDescription = nftDescription;
    }
    
    public String getImageType() {
        return imageType;
    }
    
    public void setImageType(String imageType) {
        this.imageType = imageType;
    }
    
    public Long getImageSize() {
        return imageSize;
    }
    
    public void setImageSize(Long imageSize) {
        this.imageSize = imageSize;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
    
    public Integer getImageWidth() {
        return imageWidth;
    }
    
    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }
    
    public Integer getImageHeight() {
        return imageHeight;
    }
    
    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }
    
    public String getBase64Data() {
        return base64Data;
    }
    
    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }
    
    public String getImageHash() {
        return imageHash;
    }
    
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
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
    
    public Long getUserAccountId() {
        return userAccountId;
    }
    
    public void setUserAccountId(Long userAccountId) {
        this.userAccountId = userAccountId;
    }
    
    public Long getProofFileId() {
        return proofFileId;
    }
    
    public void setProofFileId(Long proofFileId) {
        this.proofFileId = proofFileId;
    }
    
    public MintStatus getMintStatus() {
        return mintStatus;
    }
    
    public void setMintStatus(MintStatus mintStatus) {
        this.mintStatus = mintStatus;
    }
    
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
    
    public String getTokenId() {
        return tokenId;
    }
    
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }
    
    public ImageStatus getStatus() {
        return status;
    }
    
    public void setStatus(ImageStatus status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "NftImage{" +
                "id=" + id +
                ", imageName='" + imageName + '\'' +
                ", nftName='" + nftName + '\'' +
                ", imageType='" + imageType + '\'' +
                ", imageSize=" + imageSize +
                ", mintStatus=" + mintStatus +
                ", uploadTime=" + uploadTime +
                '}';
    }
    
    /**
     * NFT铸造状态枚举
     */
    public enum MintStatus {
        NOT_STARTED("未开始"),
        PROCESSING("铸造中"),
        SUCCESS("铸造成功"),
        FAILED("铸造失败");
        
        private final String description;
        
        MintStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 图片状态枚举
     */
    public enum ImageStatus {
        ACTIVE("正常"),
        DELETED("已删除");
        
        private final String description;
        
        ImageStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

