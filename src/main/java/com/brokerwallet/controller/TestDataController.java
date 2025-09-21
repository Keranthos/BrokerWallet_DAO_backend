package com.brokerwallet.controller;

import com.brokerwallet.entity.ProofFile;
import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.repository.ProofFileRepository;
import com.brokerwallet.repository.UserAccountRepository;
import com.brokerwallet.service.UserAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试数据控制器
 * 用于创建测试数据
 */
@RestController
@RequestMapping("/api/test-data")
@CrossOrigin(origins = "*")
public class TestDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataController.class);
    
    @Autowired
    private UserAccountService userAccountService;
    
    @Autowired
    private ProofFileRepository proofFileRepository;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    /**
     * 创建测试数据
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTestData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始创建测试数据");
            
            // 创建测试用户
            UserAccount user1 = userAccountService.getOrCreateUser("0x1234567890123456789012345678901234567890");
            user1.setDisplayName("测试用户1");
            user1.setRepresentativeWork("区块链研究项目");
            user1.setShowRepresentativeWork(true);
            user1.setGoldMedals(2);
            user1.setSilverMedals(3);
            user1.setBronzeMedals(1);
            userAccountService.save(user1);
            
            UserAccount user2 = userAccountService.getOrCreateUser("0x2345678901234567890123456789012345678901");
            user2.setDisplayName("测试用户2");
            user2.setRepresentativeWork("智能合约开发");
            user2.setShowRepresentativeWork(true);
            user2.setGoldMedals(1);
            user2.setSilverMedals(2);
            user2.setBronzeMedals(4);
            userAccountService.save(user2);
            
            UserAccount user3 = userAccountService.getOrCreateUser("0x3456789012345678901234567890123456789012");
            user3.setDisplayName("待审核用户");
            user3.setRepresentativeWork("DApp开发项目");
            user3.setShowRepresentativeWork(true);
            userAccountService.save(user3);
            
            // 创建测试证明文件
            ProofFile proofFile1 = new ProofFile();
            proofFile1.setUserAccountId(user3.getId());
            proofFile1.setFileName("proof_" + System.currentTimeMillis() + ".pdf");
            proofFile1.setOriginalName("研究报告.pdf");
            proofFile1.setFileType("application/pdf");
            proofFile1.setFileSize(1024000L);
            proofFile1.setFilePath("/uploads/proofs/users/" + user3.getId() + "/" + proofFile1.getFileName());
            proofFile1.setAuditStatus(ProofFile.AuditStatus.PENDING);
            proofFile1.setUploadTime(LocalDateTime.now().minusHours(2));
            proofFileRepository.save(proofFile1);
            
            ProofFile proofFile2 = new ProofFile();
            proofFile2.setUserAccountId(user3.getId());
            proofFile2.setFileName("proof_" + System.currentTimeMillis() + "_2.docx");
            proofFile2.setOriginalName("项目文档.docx");
            proofFile2.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            proofFile2.setFileSize(512000L);
            proofFile2.setFilePath("/uploads/proofs/users/" + user3.getId() + "/" + proofFile2.getFileName());
            proofFile2.setAuditStatus(ProofFile.AuditStatus.PENDING);
            proofFile2.setUploadTime(LocalDateTime.now().minusHours(1));
            proofFileRepository.save(proofFile2);
            
            // 创建另一个用户的待审核文件
            UserAccount user4 = userAccountService.getOrCreateUser("0x4567890123456789012345678901234567890123");
            user4.setDisplayName("新用户");
            user4.setRepresentativeWork("机器学习研究");
            user4.setShowRepresentativeWork(true);
            userAccountService.save(user4);
            
            ProofFile proofFile3 = new ProofFile();
            proofFile3.setUserAccountId(user4.getId());
            proofFile3.setFileName("proof_" + System.currentTimeMillis() + "_3.jpg");
            proofFile3.setOriginalName("实验截图.jpg");
            proofFile3.setFileType("image/jpeg");
            proofFile3.setFileSize(256000L);
            proofFile3.setFilePath("/uploads/proofs/users/" + user4.getId() + "/" + proofFile3.getFileName());
            proofFile3.setAuditStatus(ProofFile.AuditStatus.PENDING);
            proofFile3.setUploadTime(LocalDateTime.now().minusMinutes(30));
            proofFileRepository.save(proofFile3);
            
            response.put("success", true);
            response.put("message", "测试数据创建成功");
            response.put("data", Map.of(
                "users", 4,
                "pendingFiles", 3,
                "usersWithMedals", 2
            ));
            
            logger.info("测试数据创建完成");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建测试数据失败", e);
            response.put("success", false);
            response.put("message", "创建测试数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 清除我创建的虚假测试数据，只保留真实用户数据
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearTestData() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始清除虚假测试数据");
            
            // 只清除我们创建的测试用户的证明文件，而不是所有文件
            String[] testWalletAddresses = {
                "0x1234567890123456789012345678901234567890",
                "0x2345678901234567890123456789012345678901", 
                "0x3456789012345678901234567890123456789012",
                "0x4567890123456789012345678901234567890123"
            };
            
            int deletedFileCount = 0;
            int deletedUserCount = 0;
            
            for (String walletAddress : testWalletAddresses) {
                UserAccount testUser = userAccountService.findByWalletAddress(walletAddress);
                if (testUser != null) {
                    logger.info("找到测试用户: {} (ID: {})", walletAddress, testUser.getId());
                    
                    // 删除该测试用户的所有证明文件
                    List<ProofFile> userFiles = proofFileRepository.findByUserAccountIdOrderByUploadTimeDesc(testUser.getId());
                    for (ProofFile file : userFiles) {
                        logger.info("删除测试文件: {} (ID: {})", file.getFileName(), file.getId());
                        proofFileRepository.delete(file);
                        deletedFileCount++;
                    }
                    
                    // 删除测试用户账户
                    userAccountRepository.delete(testUser);
                    deletedUserCount++;
                    logger.info("删除测试用户: {}", walletAddress);
                }
            }
            
            response.put("success", true);
            response.put("message", "虚假测试数据清除成功，删除了 " + deletedUserCount + " 个测试用户和 " + deletedFileCount + " 个测试文件");
            response.put("data", Map.of(
                "deletedUsers", deletedUserCount,
                "deletedFiles", deletedFileCount
            ));
            
            logger.info("虚假测试数据清除完成，删除了{}个用户和{}个文件", deletedUserCount, deletedFileCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("清除虚假测试数据失败", e);
            response.put("success", false);
            response.put("message", "清除虚假测试数据失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
