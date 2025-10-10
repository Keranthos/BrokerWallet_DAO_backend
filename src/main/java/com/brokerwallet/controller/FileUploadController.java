package com.brokerwallet.controller;

import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.entity.ProofFile;
import com.brokerwallet.entity.NftImage;
import com.brokerwallet.service.AsyncFileProcessorService;
import com.brokerwallet.service.UserAccountService;
import com.brokerwallet.repository.ProofFileRepository;
import com.brokerwallet.repository.NftImageRepository;
import com.brokerwallet.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 处理证明文件和NFT图片的上传
 */
@RestController
@RequestMapping("/api/upload")
// @CrossOrigin 已在 WebConfig 中统一配置，此处删除避免冲突
public class FileUploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    
    @Autowired
    private UserAccountService userAccountService;
    
    @Autowired
    private ProofFileRepository proofFileRepository;
    
    @Autowired
    private NftImageRepository nftImageRepository;
    
    @Autowired
    private AsyncFileProcessorService asyncFileProcessorService;
    
    /**
     * 初始化方法：为旧数据生成批次ID
     */
    @jakarta.annotation.PostConstruct
    public void initLegacyBatchIds() {
        try {
            // 查找所有没有批次ID的文件
            List<ProofFile> filesWithoutBatchId = proofFileRepository.findAll().stream()
                .filter(file -> file.getSubmissionBatchId() == null)
                .toList();
            
            if (!filesWithoutBatchId.isEmpty()) {
                logger.info("发现 {} 个文件没有批次ID，开始生成...", filesWithoutBatchId.size());
                
                for (ProofFile file : filesWithoutBatchId) {
                    // 生成唯一的批次ID
                    // 格式：LEGACY_BATCH_{userId}_{fileId}_{timestamp}
                    long timestamp = file.getUploadTime().atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
                    String batchId = String.format("LEGACY_BATCH_%d_%d_%d", 
                        file.getUserAccountId(), file.getId(), timestamp);
                    
                    file.setSubmissionBatchId(batchId);
                    proofFileRepository.save(file);
                }
                
                logger.info("✅ 成功为 {} 个旧文件生成批次ID", filesWithoutBatchId.size());
            } else {
                logger.info("所有文件都已有批次ID");
            }
        } catch (Exception e) {
            logger.error("生成旧数据批次ID失败", e);
        }
    }
    
    /**
     * 连接测试接口
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Backend connection successful!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        logger.info("Received connection test request");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 完整提交：多个证明文件 + NFT图片 + 用户信息
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> uploadComplete(
            @RequestParam("proofFiles") MultipartFile[] proofFiles,
            @RequestParam(value = "nftImage", required = false) MultipartFile nftImage,
            @RequestParam("walletAddress") String walletAddress,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "representativeWork", required = false) String representativeWork,
            @RequestParam(value = "showRepresentativeWork", defaultValue = "false") boolean showRepresentativeWork) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 临时文件路径列表，用于失败时清理
        List<String> tempFilePaths = new ArrayList<>();
        
        try {
            logger.info("=== Multiple files upload request received ===");
            logger.info("Wallet address: {}", walletAddress);
            logger.info("Proof files count: {}", proofFiles.length);
            for (int i = 0; i < proofFiles.length; i++) {
                logger.info("Proof file {}: {} (size: {} bytes)", i+1, proofFiles[i].getOriginalFilename(), proofFiles[i].getSize());
            }
            logger.info("NFT image: {}", nftImage != null ? nftImage.getOriginalFilename() + " (size: " + nftImage.getSize() + " bytes)" : "Not uploaded");
            logger.info("Display name: {}", displayName);
            logger.info("Representative work: {}", representativeWork);
            logger.info("Show representative work: {}", showRepresentativeWork);
            
            // 1. 先获取或创建用户账户（数据库操作）
            UserAccount user = userAccountService.getOrCreateUser(walletAddress);
            logger.info("User account ready: ID={}", user.getId());
            
            // 2. 保存所有证明文件到文件系统
            String userProofDir = FileUtil.getUserProofDirectory(user.getId());
            List<String> proofFilePaths = new ArrayList<>();
            
            for (int i = 0; i < proofFiles.length; i++) {
                MultipartFile proofFile = proofFiles[i];
                String proofFilePath = FileUtil.saveFile(proofFile, userProofDir);
                proofFilePaths.add(proofFilePath);
                tempFilePaths.add(proofFilePath);
                logger.info("Proof file {} saved to: {}", i+1, proofFilePath);
            }
            
            // 3. 检查NFT图片唯一性并保存到文件系统（如果有）
            String nftImagePath = null;
            String nftImageHash = null;
            if (nftImage != null && !nftImage.isEmpty()) {
                // 计算NFT图片的SHA-256哈希值
                nftImageHash = calculateImageHash(nftImage);
                logger.info("NFT image hash calculated: {}", nftImageHash);
                
                // 检查哈希是否已存在（仅针对用户上传的NFT图片）
                if (proofFileRepository.existsByNftImageHash(nftImageHash)) {
                    logger.warn("NFT image already exists with hash: {}", nftImageHash);
                    response.put("success", false);
                    response.put("message", "该NFT图片已存在，请上传不同的图片");
                    response.put("errorCode", "DUPLICATE_NFT_IMAGE");
                    
                    // 清理已上传的证明文件
                    for (String tempPath : tempFilePaths) {
                        FileUtil.deleteFile(tempPath);
                    }
                    
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 保存NFT图片到文件系统
                String userNftDir = FileUtil.getUserNftDirectory(user.getId());
                nftImagePath = FileUtil.saveFile(nftImage, userNftDir);
                tempFilePaths.add(nftImagePath);
                logger.info("NFT image saved to: {}", nftImagePath);
            }
            
            // 4. 更新用户信息
            if (displayName != null && !displayName.trim().isEmpty()) {
                user.setDisplayName(displayName.trim());
            }
            if (representativeWork != null && !representativeWork.trim().isEmpty()) {
                user.setRepresentativeWork(representativeWork.trim());
            }
            user.setShowRepresentativeWork(showRepresentativeWork);
            user.setUpdateTime(LocalDateTime.now());
            userAccountService.save(user);
            
            // 5. 生成提交批次ID（用于标识同一次提交的多个文件）
            String submissionBatchId = "BATCH_" + user.getId() + "_" + System.currentTimeMillis();
            logger.info("Generated submission batch ID: {}", submissionBatchId);
            
            // 6. 保存所有证明文件到数据库
            List<ProofFile> savedProofFiles = new ArrayList<>();
            for (int i = 0; i < proofFiles.length; i++) {
                MultipartFile proofFile = proofFiles[i];
                String proofFilePath = proofFilePaths.get(i);
                ProofFile savedProofFile = saveProofFileToDatabase(proofFile, proofFilePath, user.getId());
                
                // 设置提交批次ID
                savedProofFile.setSubmissionBatchId(submissionBatchId);
                
                // 如果是第一个证明文件且有NFT图片，保存NFT图片哈希
                if (i == 0 && nftImageHash != null) {
                    savedProofFile.setNftImageHash(nftImageHash);
                }
                
                proofFileRepository.save(savedProofFile);
                logger.info("Proof file {} database record created: ID={}, BatchID={}", 
                           i+1, savedProofFile.getId(), submissionBatchId);
                
                savedProofFiles.add(savedProofFile);
            }
            
            // 6. 保存NFT图片到数据库（如果有）
            NftImage savedNftImage = null;
            if (nftImage != null && !nftImage.isEmpty() && nftImagePath != null) {
                // NFT图片关联到第一个证明文件
                Long firstProofFileId = savedProofFiles.get(0).getId();
                savedNftImage = saveNftImageToDatabase(nftImage, nftImagePath, user.getId(), firstProofFileId);
            }
            
            // 7. 构建详细响应（为Android端优化）
            Map<String, Object> data = new HashMap<>();
            
            // 使用批次ID作为提交ID
            data.put("submissionId", submissionBatchId);
            data.put("submissionBatchId", submissionBatchId);
            data.put("status", "PENDING");  // 初始状态为待审核
            data.put("submitTime", LocalDateTime.now().toString());
            
            // 用户信息
            data.put("user", Map.of(
                    "id", user.getId(),
                    "walletAddress", user.getWalletAddress(),
                    "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                    "representativeWork", user.getRepresentativeWork() != null ? user.getRepresentativeWork() : "",
                    "showRepresentativeWork", user.getShowRepresentativeWork()
            ));
            
            // 构建证明文件列表
            List<Map<String, Object>> proofFilesList = new ArrayList<>();
            for (ProofFile proofFile : savedProofFiles) {
                proofFilesList.add(Map.of(
                    "id", proofFile.getId(),
                    "fileName", proofFile.getFileName(),
                    "originalName", proofFile.getOriginalName(),
                    "fileSize", proofFile.getFileSize(),
                    "fileType", proofFile.getFileType(),
                    "uploadTime", proofFile.getUploadTime().toString(),
                    "auditStatus", proofFile.getAuditStatus().name()
                ));
            }
            data.put("proofFiles", proofFilesList);
            data.put("totalProofFiles", proofFilesList.size());
            
            // NFT图片信息
            if (savedNftImage != null) {
                data.put("nftImage", Map.of(
                        "id", savedNftImage.getId(),
                        "imageName", savedNftImage.getImageName(),
                        "originalName", savedNftImage.getOriginalName(),
                        "imageSize", savedNftImage.getImageSize(),
                        "imageType", savedNftImage.getImageType(),
                        "uploadTime", savedNftImage.getUploadTime().toString(),
                        "mintStatus", savedNftImage.getMintStatus().name()
                ));
                data.put("hasNftImage", true);
            } else {
                data.put("hasNftImage", false);
            }
            
            // 下一步提示
            data.put("nextSteps", List.of(
                "您的材料已成功提交到系统",
                "管理员将在24小时内进行审核",
                "审核完成后您将获得相应的勋章",
                "可以在勋章排行榜中查看审核进度"
            ));
            
            response.put("success", true);
            response.put("message", "提交成功！您的证明材料已收到，请等待管理员审核。");
            response.put("data", data);
            
            logger.info("Multiple files submission successful - User ID: {}, Proof files count: {}", user.getId(), savedProofFiles.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Complete submission failed: ", e);
            
            // 清理已保存的临时文件
            for (String filePath : tempFilePaths) {
                try {
                    if (FileUtil.fileExists(filePath)) {
                        FileUtil.deleteFile(filePath);
                        logger.info("Cleaned up temp file: {}", filePath);
                    }
                } catch (Exception cleanupException) {
                    logger.warn("Failed to cleanup file: {}", filePath, cleanupException);
                }
            }
            
            response.put("success", false);
            response.put("message", "File upload failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 计算图片的SHA-256哈希值
     */
    private String calculateImageHash(MultipartFile file) throws Exception {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.error("Failed to calculate image hash", e);
            throw new Exception("计算图片哈希失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存证明文件到数据库
     */
    private ProofFile saveProofFileToDatabase(MultipartFile file, String filePath, Long userAccountId) throws Exception {
        // 文件已经保存到文件系统，只需要创建数据库记录
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        
        logger.info("Creating database record for proof file: {}", filePath);
        
        // 创建数据库记录
        ProofFile proofFile = new ProofFile();
        proofFile.setUserAccountId(userAccountId);
        proofFile.setFileName(fileName);
        proofFile.setFilePath(filePath);  // 设置文件路径
        proofFile.setOriginalName(file.getOriginalFilename());
        proofFile.setFileType(file.getContentType());
        proofFile.setFileSize(file.getSize());
        proofFile.setUploadTime(LocalDateTime.now());
        proofFile.setAuditStatus(ProofFile.AuditStatus.PENDING);
        proofFile.setMedalAwarded(ProofFile.MedalType.NONE);
        proofFile.setStatus(ProofFile.FileStatus.ACTIVE);
        
        ProofFile savedProofFile = proofFileRepository.save(proofFile);
        
        // 异步处理文件哈希计算
        asyncFileProcessorService.calculateFileHashAsync(filePath);
        
        return savedProofFile;
    }
    
    /**
     * 保存NFT图片到数据库
     */
    private NftImage saveNftImageToDatabase(MultipartFile file, String imagePath, Long userAccountId, Long proofFileId) throws Exception {
        // 文件已经保存到文件系统，只需要创建数据库记录
        String imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
        
        logger.info("Creating database record for NFT image: {}", imagePath);
        
        // 创建数据库记录
        NftImage nftImage = new NftImage();
        nftImage.setUserAccountId(userAccountId);
        nftImage.setProofFileId(proofFileId);
        nftImage.setImageName(imageName);
        nftImage.setImagePath(imagePath);  // 设置图片路径
        nftImage.setOriginalName(file.getOriginalFilename());
        nftImage.setImageType(file.getContentType());
        nftImage.setImageSize(file.getSize());
        nftImage.setUploadTime(LocalDateTime.now());
        nftImage.setMintStatus(NftImage.MintStatus.NOT_STARTED);
        nftImage.setStatus(NftImage.ImageStatus.ACTIVE);
        
        NftImage savedNftImage = nftImageRepository.save(nftImage);
        
        // 异步生成缩略图
        String thumbnailDir = FileUtil.getUserThumbnailDirectory(userAccountId);
        String thumbnailPath = thumbnailDir + "thumb_" + imageName;
        asyncFileProcessorService.generateThumbnailAsync(imagePath, thumbnailPath, 300, 300);
        
        // 异步计算文件哈希
        asyncFileProcessorService.calculateFileHashAsync(imagePath);
        
        return savedNftImage;
    }
    
    /**
     * 获取用户提交历史
     */
    @GetMapping("/user/submissions")
    public ResponseEntity<Map<String, Object>> getUserSubmissions(
            @RequestParam String walletAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取用户提交历史: {}, page={}, size={}", walletAddress, page, size);
            
            // 规范化钱包地址（尝试多种格式）
            UserAccount user = null;
            
            // 1. 先尝试原始地址
            user = userAccountService.findByWalletAddress(walletAddress);
            
            // 2. 如果失败，尝试添加0x前缀
            if (user == null && !walletAddress.startsWith("0x")) {
                user = userAccountService.findByWalletAddress("0x" + walletAddress);
            }
            
            // 3. 如果失败，尝试去掉0x前缀
            if (user == null && walletAddress.startsWith("0x")) {
                user = userAccountService.findByWalletAddress(walletAddress.substring(2));
            }
            
            if (user == null) {
                logger.info("用户不存在，返回空提交历史: {}", walletAddress);
                // 返回空数组而不是404错误，让手机端显示"暂无提交历史"
                response.put("success", true);
                response.put("data", new ArrayList<>());
                response.put("pagination", Map.of(
                    "currentPage", page,
                    "pageSize", size,
                    "totalItems", 0,
                    "totalPages", 0
                ));
                return ResponseEntity.ok(response);
            }
            
            logger.info("找到用户: {}, displayName={}", user.getWalletAddress(), user.getDisplayName());
            
            // 获取用户的所有证明文件（按时间倒序）
            List<ProofFile> allProofFiles = proofFileRepository.findByUserAccountIdOrderByUploadTimeDesc(user.getId());
            
            // 按批次分组（使用LinkedHashMap保持顺序）
            java.util.LinkedHashMap<String, List<ProofFile>> batchMap = new java.util.LinkedHashMap<>();
            for (ProofFile file : allProofFiles) {
                String batchId = file.getSubmissionBatchId();
                if (batchId == null) {
                    // 旧数据没有批次ID，使用文件ID作为唯一批次
                    batchId = "LEGACY_" + file.getId();
                }
                batchMap.computeIfAbsent(batchId, k -> new ArrayList<>()).add(file);
            }
            
            // 获取批次列表（已按时间排序）
            List<List<ProofFile>> allBatches = new ArrayList<>(batchMap.values());
            
            // 简单分页处理
            int start = page * size;
            int end = Math.min(start + size, allBatches.size());
            List<List<ProofFile>> pagedBatches = allBatches.subList(start, end);
            
            List<Map<String, Object>> submissions = new ArrayList<>();
            for (List<ProofFile> batchFiles : pagedBatches) {
                // 使用批次中的第一个文件作为代表
                ProofFile proofFile = batchFiles.get(0);
                Map<String, Object> submission = new HashMap<>();
                
                // 基本信息
                submission.put("submissionId", proofFile.getSubmissionBatchId() != null ? 
                    proofFile.getSubmissionBatchId() : "SUB_" + proofFile.getId() + "_" + proofFile.getUploadTime().toString().hashCode());
                submission.put("id", proofFile.getId());
                submission.put("batchId", proofFile.getSubmissionBatchId());
                submission.put("fileCount", batchFiles.size());  // 该批次的文件数量
                submission.put("fileName", proofFile.getOriginalName());
                submission.put("fileSize", proofFile.getFileSize());
                submission.put("fileType", proofFile.getFileType());
                submission.put("uploadTime", proofFile.getUploadTime().toString());
                
                // 审核状态
                submission.put("auditStatus", proofFile.getAuditStatus().name());
                submission.put("auditStatusDesc", getAuditStatusDescription(proofFile.getAuditStatus()));
                if (proofFile.getAuditTime() != null) {
                    submission.put("auditTime", proofFile.getAuditTime().toString());
                }
                
                // 勋章信息（处理null值）
                ProofFile.MedalType medalAwarded = proofFile.getMedalAwarded();
                if (medalAwarded != null) {
                    submission.put("medalAwarded", medalAwarded.name());
                    submission.put("medalAwardedDesc", getMedalDescription(medalAwarded));
                } else {
                    submission.put("medalAwarded", "NONE");
                    submission.put("medalAwardedDesc", "无");
                }
                if (proofFile.getMedalAwardTime() != null) {
                    submission.put("medalAwardTime", proofFile.getMedalAwardTime().toString());
                }
                if (proofFile.getMedalTransactionHash() != null) {
                    submission.put("medalTransactionHash", proofFile.getMedalTransactionHash());
                }
                
                // 代币奖励信息
                if (proofFile.getTokenReward() != null && proofFile.getTokenReward().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    submission.put("tokenReward", proofFile.getTokenReward().toString());
                    submission.put("tokenRewardTxHash", proofFile.getTokenRewardTxHash());
                } else {
                    submission.put("tokenReward", null);
                    submission.put("tokenRewardTxHash", null);
                }
                
                // 用户信息（前端需要）
                submission.put("user", Map.of(
                    "id", user.getId(),
                    "walletAddress", user.getWalletAddress(),
                    "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                    "representativeWork", user.getRepresentativeWork() != null ? user.getRepresentativeWork() : "",
                    "showRepresentativeWork", user.getShowRepresentativeWork(),
                    "totalMedals", user.getGoldMedals() + user.getSilverMedals() + user.getBronzeMedals(),
                    "goldMedals", user.getGoldMedals(),
                    "silverMedals", user.getSilverMedals(),
                    "bronzeMedals", user.getBronzeMedals()
                ));
                
                // 关联的NFT图片
                List<NftImage> nftImages = nftImageRepository.findByProofFileId(proofFile.getId());
                if (!nftImages.isEmpty()) {
                    NftImage nftImage = nftImages.get(0);
                    submission.put("nftImage", Map.of(
                        "id", nftImage.getId(),
                        "originalName", nftImage.getOriginalName(),
                        "mintStatus", nftImage.getMintStatus().name(),
                        "mintStatusDesc", getMintStatusDescription(nftImage.getMintStatus()),
                        "tokenId", nftImage.getTokenId() != null ? nftImage.getTokenId() : "",
                        "transactionHash", nftImage.getTransactionHash() != null ? nftImage.getTransactionHash() : ""
                    ));
                }
                
                submissions.add(submission);
            }
            
            response.put("success", true);
            response.put("data", submissions);
            response.put("pagination", Map.of(
                "currentPage", page,
                "pageSize", size,
                "totalItems", allBatches.size(),  // 使用批次数量
                "totalPages", (int) Math.ceil((double) allBatches.size() / size)
            ));
            
            logger.info("返回 {} 个提交批次（共 {} 个文件）", submissions.size(), allProofFiles.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取用户提交历史失败", e);
            response.put("success", false);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取单个提交的详细信息
     */
    @GetMapping("/submission/detail/{id}")
    public ResponseEntity<Map<String, Object>> getSubmissionDetail(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取提交详情: {}", id);
            
            // 查找证明文件
            ProofFile proofFile = proofFileRepository.findById(id).orElse(null);
            if (proofFile == null) {
                response.put("success", false);
                response.put("message", "提交记录不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            // 获取用户信息
            UserAccount user = userAccountService.findById(proofFile.getUserAccountId());
            
            Map<String, Object> detail = new HashMap<>();
            detail.put("submissionId", "SUB_" + proofFile.getId() + "_" + proofFile.getUploadTime().toString().hashCode());
            detail.put("id", proofFile.getId());
            detail.put("fileName", proofFile.getOriginalName());
            detail.put("fileSize", proofFile.getFileSize());
            detail.put("fileType", proofFile.getFileType());
            detail.put("uploadTime", proofFile.getUploadTime().toString());
            
            // 审核状态和详细信息
            detail.put("auditStatus", proofFile.getAuditStatus().name());
            detail.put("auditStatusDesc", getAuditStatusDescription(proofFile.getAuditStatus()));
            if (proofFile.getAuditTime() != null) {
                detail.put("auditTime", proofFile.getAuditTime().toString());
            }
            
            // 勋章信息
            detail.put("medalAwarded", proofFile.getMedalAwarded().name());
            detail.put("medalAwardedDesc", getMedalDescription(proofFile.getMedalAwarded()));
            if (proofFile.getMedalAwardTime() != null) {
                detail.put("medalAwardTime", proofFile.getMedalAwardTime().toString());
            }
            
            // 用户信息
            if (user != null) {
                detail.put("user", Map.of(
                    "walletAddress", user.getWalletAddress(),
                    "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                    "totalMedals", user.getGoldMedals() + user.getSilverMedals() + user.getBronzeMedals()
                ));
            }
            
            // 处理进度
            List<String> processSteps = new ArrayList<>();
            processSteps.add("✅ 文件已上传");
            
            switch (proofFile.getAuditStatus()) {
                case PENDING:
                    processSteps.add("⏳ 等待管理员审核");
                    processSteps.add("⏸️ 勋章分配");
                    break;
                case APPROVED:
                    processSteps.add("✅ 审核通过");
                    if (proofFile.getMedalAwarded() != ProofFile.MedalType.NONE) {
                        processSteps.add("✅ 勋章已分配: " + getMedalDescription(proofFile.getMedalAwarded()));
                    } else {
                        processSteps.add("⏳ 等待勋章分配");
                    }
                    break;
                case REJECTED:
                    processSteps.add("❌ 审核未通过");
                    break;
            }
            
            detail.put("processSteps", processSteps);
            
            response.put("success", true);
            response.put("data", detail);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取提交详情失败", e);
            response.put("success", false);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取审核状态描述
     */
    private String getAuditStatusDescription(ProofFile.AuditStatus status) {
        switch (status) {
            case PENDING:
                return "Pending";
            case APPROVED:
                return "Approved";
            case REJECTED:
                return "Rejected";
            default:
                return status.name();
        }
    }
    
    /**
     * 获取勋章描述
     */
    private String getMedalDescription(ProofFile.MedalType medal) {
        switch (medal) {
            case GOLD:
                return "金牌";
            case SILVER:
                return "银牌";
            case BRONZE:
                return "铜牌";
            case NONE:
                return "无勋章";
            default:
                return medal.name();
        }
    }
    
    /**
     * 获取NFT铸造状态描述
     */
    private String getMintStatusDescription(NftImage.MintStatus status) {
        switch (status) {
            case NOT_STARTED:
                return "未开始";
            case PROCESSING:
                return "铸造中";
            case SUCCESS:
                return "铸造成功";
            case FAILED:
                return "铸造失败";
            default:
                return status.name();
        }
    }
}
