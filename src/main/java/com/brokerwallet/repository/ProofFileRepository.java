package com.brokerwallet.repository;

import com.brokerwallet.entity.ProofFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 证明文件数据访问层
 * 提供证明文件的CRUD操作
 */
@Repository
public interface ProofFileRepository extends JpaRepository<ProofFile, Long> {
    
    /**
     * 根据文件名查找文件
     */
    Optional<ProofFile> findByFileName(String fileName);
    
    /**
     * 根据文件名查找文件（返回单个对象）
     */
    ProofFile findFirstByFileName(String fileName);
    
    /**
     * 根据文件哈希查找文件（用于防重复上传）
     */
    Optional<ProofFile> findByFileHash(String fileHash);
    
    /**
     * 根据用户账户ID查找文件列表
     */
    List<ProofFile> findByUserAccountIdOrderByUploadTimeDesc(Long userAccountId);
    
    /**
     * 根据状态查找文件列表
     */
    List<ProofFile> findByStatusOrderByUploadTimeDesc(ProofFile.FileStatus status);
    
    /**
     * 根据用户账户ID和状态查找文件列表
     */
    List<ProofFile> findByUserAccountIdAndStatusOrderByUploadTimeDesc(Long userAccountId, ProofFile.FileStatus status);
    
    /**
     * 根据文件类型查找文件列表
     */
    List<ProofFile> findByFileTypeContainingIgnoreCaseOrderByUploadTimeDesc(String fileType);
    
    /**
     * 根据时间范围查找文件
     */
    @Query("SELECT p FROM ProofFile p WHERE p.uploadTime BETWEEN :startTime AND :endTime ORDER BY p.uploadTime DESC")
    List<ProofFile> findByUploadTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户上传的文件数量
     */
    long countByUserAccountIdAndStatus(Long userAccountId, ProofFile.FileStatus status);
    
    /**
     * 统计总文件数量
     */
    long countByStatus(ProofFile.FileStatus status);
    
    /**
     * 获取用户上传的文件总大小
     */
    @Query("SELECT COALESCE(SUM(p.fileSize), 0) FROM ProofFile p WHERE p.userAccountId = :userAccountId AND p.status = :status")
    long getTotalFileSizeByUser(@Param("userAccountId") Long userAccountId, @Param("status") ProofFile.FileStatus status);
    
    /**
     * 查找最近上传的文件
     */
    List<ProofFile> findTop10ByStatusOrderByUploadTimeDesc(ProofFile.FileStatus status);
    
    /**
     * 根据文件大小范围查找文件
     */
    @Query("SELECT p FROM ProofFile p WHERE p.fileSize BETWEEN :minSize AND :maxSize AND p.status = :status ORDER BY p.uploadTime DESC")
    List<ProofFile> findByFileSizeBetween(@Param("minSize") Long minSize, 
                                         @Param("maxSize") Long maxSize, 
                                         @Param("status") ProofFile.FileStatus status);
    
    /**
     * 模糊搜索文件名或原始文件名
     */
    @Query("SELECT p FROM ProofFile p WHERE (p.fileName LIKE %:keyword% OR p.originalName LIKE %:keyword%) AND p.status = :status ORDER BY p.uploadTime DESC")
    List<ProofFile> searchByKeyword(@Param("keyword") String keyword, @Param("status") ProofFile.FileStatus status);
    
    // 审核状态相关查询方法
    
    /**
     * 根据审核状态查找文件（分页）
     */
    Page<ProofFile> findByAuditStatus(ProofFile.AuditStatus auditStatus, Pageable pageable);
    
    /**
     * 根据多个审核状态查找文件（分页）
     */
    Page<ProofFile> findByAuditStatusIn(List<ProofFile.AuditStatus> auditStatuses, Pageable pageable);
    
    /**
     * 根据用户ID和审核状态查找文件
     */
    List<ProofFile> findByUserAccountIdAndAuditStatus(Long userAccountId, ProofFile.AuditStatus auditStatus);
    
    /**
     * 统计各审核状态的文件数量
     */
    long countByAuditStatus(ProofFile.AuditStatus auditStatus);
    
    /**
     * 根据勋章类型查找文件
     */
    List<ProofFile> findByMedalAwarded(ProofFile.MedalType medalType);
    
    /**
     * 根据NFT图片哈希查找文件（用于检查NFT图片唯一性）
     */
    Optional<ProofFile> findByNftImageHash(String nftImageHash);
    
    /**
     * 检查NFT图片哈希是否已存在
     */
    boolean existsByNftImageHash(String nftImageHash);
    
    /**
     * 根据提交批次ID查找文件列表
     */
    List<ProofFile> findBySubmissionBatchIdOrderByUploadTimeAsc(String submissionBatchId);
    
    /**
     * 查找所有待审核的批次（按批次分组，返回批次ID列表）
     * 使用子查询来实现按时间排序
     */
    @Query("SELECT p.submissionBatchId FROM ProofFile p WHERE p.auditStatus = :auditStatus AND p.submissionBatchId IS NOT NULL " +
           "GROUP BY p.submissionBatchId ORDER BY MAX(p.uploadTime) DESC")
    List<String> findDistinctBatchIdsByAuditStatus(@Param("auditStatus") ProofFile.AuditStatus auditStatus);
    
    /**
     * 查找所有批次（不区分审核状态，按时间倒序）
     */
    @Query("SELECT p.submissionBatchId FROM ProofFile p WHERE p.submissionBatchId IS NOT NULL " +
           "GROUP BY p.submissionBatchId ORDER BY MAX(p.uploadTime) DESC")
    List<String> findAllDistinctBatchIds();
}

