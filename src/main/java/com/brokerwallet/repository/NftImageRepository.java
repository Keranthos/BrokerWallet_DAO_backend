package com.brokerwallet.repository;

import com.brokerwallet.entity.NftImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * NFT图片数据访问层
 * 提供NFT图片的CRUD操作
 */
@Repository
public interface NftImageRepository extends JpaRepository<NftImage, Long> {
    
    /**
     * 根据图片名查找
     */
    Optional<NftImage> findByImageName(String imageName);
    
    /**
     * 根据图片哈希查找（用于防重复上传）
     */
    Optional<NftImage> findByImageHash(String imageHash);
    
    /**
     * 根据用户账户ID查找图片列表
     */
    List<NftImage> findByUserAccountIdOrderByUploadTimeDesc(Long userAccountId);
    
    /**
     * 根据证明文件ID查找关联的NFT图片
     */
    List<NftImage> findByProofFileId(Long proofFileId);
    
    /**
     * 根据用户账户ID和状态查找图片列表
     */
    List<NftImage> findByUserAccountIdAndStatusOrderByUploadTimeDesc(Long userAccountId, NftImage.ImageStatus status);
    
    /**
     * 根据铸造状态查找图片列表
     */
    List<NftImage> findByMintStatusOrderByUploadTimeDesc(NftImage.MintStatus mintStatus);
    
    /**
     * 根据用户账户ID和铸造状态查找图片列表
     */
    List<NftImage> findByUserAccountIdAndMintStatusOrderByUploadTimeDesc(Long userAccountId, NftImage.MintStatus mintStatus);
    
    /**
     * 根据NFT名称查找
     */
    List<NftImage> findByNftNameContainingIgnoreCaseOrderByUploadTimeDesc(String nftName);
    
    /**
     * 根据交易哈希查找NFT
     */
    Optional<NftImage> findByTransactionHash(String transactionHash);
    
    /**
     * 根据Token ID查找NFT
     */
    Optional<NftImage> findByTokenId(String tokenId);
    
    /**
     * 根据时间范围查找图片
     */
    @Query("SELECT n FROM NftImage n WHERE n.uploadTime BETWEEN :startTime AND :endTime ORDER BY n.uploadTime DESC")
    List<NftImage> findByUploadTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户的NFT数量（按状态）
     */
    long countByUserAccountIdAndMintStatus(Long userAccountId, NftImage.MintStatus mintStatus);
    
    /**
     * 统计总NFT数量（按铸造状态）
     */
    long countByMintStatus(NftImage.MintStatus mintStatus);
    
    /**
     * 统计用户上传的图片总大小
     */
    @Query("SELECT COALESCE(SUM(n.imageSize), 0) FROM NftImage n WHERE n.userAccountId = :userAccountId AND n.status = :status")
    long getTotalImageSizeByUser(@Param("userAccountId") Long userAccountId, @Param("status") NftImage.ImageStatus status);
    
    /**
     * 查找最近上传的图片
     */
    List<NftImage> findTop10ByStatusOrderByUploadTimeDesc(NftImage.ImageStatus status);
    
    /**
     * 查找成功铸造的NFT
     */
    List<NftImage> findByMintStatusAndStatusOrderByUploadTimeDesc(NftImage.MintStatus mintStatus, NftImage.ImageStatus status);
    
    /**
     * 根据图片尺寸范围查找
     */
    @Query("SELECT n FROM NftImage n WHERE n.imageWidth >= :minWidth AND n.imageHeight >= :minHeight AND n.status = :status ORDER BY n.uploadTime DESC")
    List<NftImage> findByImageSizeRange(@Param("minWidth") Integer minWidth, 
                                       @Param("minHeight") Integer minHeight, 
                                       @Param("status") NftImage.ImageStatus status);
    
    /**
     * 模糊搜索NFT名称或描述
     */
    @Query("SELECT n FROM NftImage n WHERE (n.nftName LIKE %:keyword% OR n.nftDescription LIKE %:keyword%) AND n.status = :status ORDER BY n.uploadTime DESC")
    List<NftImage> searchByKeyword(@Param("keyword") String keyword, @Param("status") NftImage.ImageStatus status);
    
    /**
     * 查找待处理的铸造任务
     */
    List<NftImage> findByMintStatusInOrderByUploadTimeAsc(List<NftImage.MintStatus> statuses);
}

