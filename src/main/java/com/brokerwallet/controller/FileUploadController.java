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
@CrossOrigin(origins = "*")
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
     * 连接测试接口
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "后端连接成功！");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        logger.info("收到连接测试请求");
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
            
            // 3. 保存NFT图片到文件系统（如果有）
            String nftImagePath = null;
            if (nftImage != null && !nftImage.isEmpty()) {
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
            
            // 5. 保存所有证明文件到数据库
            List<ProofFile> savedProofFiles = new ArrayList<>();
            for (int i = 0; i < proofFiles.length; i++) {
                MultipartFile proofFile = proofFiles[i];
                String proofFilePath = proofFilePaths.get(i);
                ProofFile savedProofFile = saveProofFileToDatabase(proofFile, proofFilePath, user.getId());
                savedProofFiles.add(savedProofFile);
                logger.info("Proof file {} database record created: ID={}", i+1, savedProofFile.getId());
            }
            
            // 6. 保存NFT图片到数据库（如果有）
            NftImage savedNftImage = null;
            if (nftImage != null && !nftImage.isEmpty() && nftImagePath != null) {
                // NFT图片关联到第一个证明文件
                Long firstProofFileId = savedProofFiles.get(0).getId();
                savedNftImage = saveNftImageToDatabase(nftImage, nftImagePath, user.getId(), firstProofFileId);
            }
            
            // 5. 构建响应
            Map<String, Object> data = new HashMap<>();
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
                    "uploadTime", proofFile.getUploadTime().toString()
                ));
            }
            data.put("proofFiles", proofFilesList);
            
            if (savedNftImage != null) {
                data.put("nftImage", Map.of(
                        "id", savedNftImage.getId(),
                        "imageName", savedNftImage.getImageName(),
                        "originalName", savedNftImage.getOriginalName(),
                        "imageSize", savedNftImage.getImageSize(),
                        "uploadTime", savedNftImage.getUploadTime().toString()
                ));
            }
            
            response.put("success", true);
            response.put("message", "文件上传成功！");
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
}
