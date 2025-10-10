package com.brokerwallet.controller;

import com.brokerwallet.entity.ProofFile;
import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.repository.ProofFileRepository;
import com.brokerwallet.repository.UserAccountRepository;
import com.brokerwallet.service.UserAccountService;
import com.brokerwallet.service.BlockchainService;
import com.brokerwallet.service.BlockchainSyncService;
import com.brokerwallet.dto.DistributeRequest;
import com.brokerwallet.dto.DistributeResponse;
import com.brokerwallet.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员控制器
 * 处理管理员相关的操作：审核文件、分配勋章、下载文件等
 */
@RestController
@RequestMapping("/api/admin")
// @CrossOrigin 已在 WebConfig 中统一配置，此处删除避免冲突
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private ProofFileRepository proofFileRepository;
    
    @Autowired
    private com.brokerwallet.util.MedalImageGenerator medalImageGenerator;
    
    @Autowired
    private BlockchainService blockchainService;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private UserAccountService userAccountService;
    
    @Autowired
    private BlockchainSyncService blockchainSyncService;
    
    @Autowired
    private com.brokerwallet.repository.NftImageRepository nftImageRepository;
    
    /**
     * 检查后端账户状态
     */
    @GetMapping("/account-status")
    public ResponseEntity<Map<String, Object>> checkAccountStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("检查后端账户状态");
            
            String accountAddress = blockchainService.getAccountAddress();
            
            // 测试连接
            boolean connected = false;
            BigInteger balance = BigInteger.ZERO;
            boolean hasNftPermission = false;
            String mintFee = "0";
            
            try {
                // 测试区块链连接
                String testResult = blockchainService.testContractConnection();
                connected = testResult.contains("successfully");
                
                // 获取余额
                org.web3j.protocol.core.methods.response.EthGetBalance balanceResponse = 
                    blockchainService.getWeb3j().ethGetBalance(accountAddress, 
                        org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
                
                if (!balanceResponse.hasError()) {
                    balance = balanceResponse.getBalance();
                }
                
                // 检查NFT铸造权限
                hasNftPermission = blockchainService.hasMintPermission(accountAddress);
                
                // 获取NFT铸造费用
                java.math.BigInteger mintFeeWei = blockchainService.getMintFee();
                double mintFeeEth = mintFeeWei.doubleValue() / 1e18;
                mintFee = String.format("%.6f", mintFeeEth);
                
            } catch (Exception e) {
                logger.error("检查账户状态失败", e);
                connected = false;
            }
            
            // 转换余额为ETH
            double balanceEth = balance.doubleValue() / 1e18;
            
            Map<String, Object> data = new HashMap<>();
            data.put("connected", connected);
            data.put("address", accountAddress);
            data.put("balance", balanceEth);
            data.put("balanceWei", balance.toString());
            data.put("hasNftPermission", hasNftPermission);
            data.put("mintFee", mintFee);
            data.put("timestamp", java.time.LocalDateTime.now().toString());
            
            response.put("success", true);
            response.put("data", data);
            
            logger.info("账户状态检查完成: connected={}, balance={} ETH, hasPermission={}", 
                connected, balanceEth, hasNftPermission);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查账户状态失败", e);
            response.put("success", false);
            response.put("message", "检查失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取待审核的文件列表（按批次分组）
     */
    @GetMapping("/pending-users")
    public ResponseEntity<Map<String, Object>> getPendingUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取待审核批次列表: page={}, limit={}", page, limit);
            
            // 查询所有待审核的批次ID
            List<String> allBatchIds = proofFileRepository.findDistinctBatchIdsByAuditStatus(
                ProofFile.AuditStatus.PENDING);
            
            logger.info("找到 {} 个待审核批次", allBatchIds.size());
            
            // 手动分页
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, allBatchIds.size());
            List<String> pagedBatchIds = allBatchIds.subList(start, end);
            
            List<Map<String, Object>> users = new ArrayList<>();
            
            for (String batchId : pagedBatchIds) {
                // 获取该批次的所有文件
                List<ProofFile> batchFiles = proofFileRepository.findBySubmissionBatchIdOrderByUploadTimeAsc(batchId);
                if (batchFiles.isEmpty()) continue;
                
                // 使用第一个文件的信息作为批次代表
                ProofFile firstFile = batchFiles.get(0);
                
                // 获取用户信息
                UserAccount user = userAccountService.findById(firstFile.getUserAccountId());
                if (user == null) continue;
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", firstFile.getId());  // 使用第一个文件的ID
                userInfo.put("batchId", batchId);  // 添加批次ID
                userInfo.put("fileCount", batchFiles.size());  // 添加文件数量
                userInfo.put("username", user.getDisplayName() != null ? user.getDisplayName() : user.getWalletAddress());
                userInfo.put("email", user.getWalletAddress()); // 使用钱包地址作为邮箱
                userInfo.put("originalFilename", firstFile.getOriginalName());
                userInfo.put("fileName", firstFile.getFileName());
                userInfo.put("fileSize", firstFile.getFileSize());
                userInfo.put("uploadTime", firstFile.getUploadTime().toString());
                userInfo.put("auditStatus", firstFile.getAuditStatus().getDescription());
                userInfo.put("objectKey", firstFile.getFileName()); // 用于下载
                userInfo.put("filePath", firstFile.getFilePath());
                userInfo.put("userAccountId", firstFile.getUserAccountId());
                
                users.add(userInfo);
            }
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) allBatchIds.size() / limit);
            
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "获取成功");
            response.put("users", users);
            response.put("total", allBatchIds.size());  // 总批次数
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            
            logger.info("成功获取{}个待审核批次", users.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取待审核文件列表失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取已审核的文件列表（包括通过和拒绝，按批次分组）
     */
    @GetMapping("/approved-users")
    public ResponseEntity<Map<String, Object>> getApprovedUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取已审核批次列表: page={}, limit={}", page, limit);
            
            // 查询所有已审核的批次ID（包括通过和拒绝）
            List<String> approvedBatchIds = proofFileRepository.findDistinctBatchIdsByAuditStatus(
                ProofFile.AuditStatus.APPROVED);
            List<String> rejectedBatchIds = proofFileRepository.findDistinctBatchIdsByAuditStatus(
                ProofFile.AuditStatus.REJECTED);
            
            // 合并两个列表
            List<String> allBatchIds = new ArrayList<>();
            allBatchIds.addAll(approvedBatchIds);
            allBatchIds.addAll(rejectedBatchIds);
            
            logger.info("找到 {} 个已审核批次（通过:{}, 拒绝:{}）", 
                allBatchIds.size(), approvedBatchIds.size(), rejectedBatchIds.size());
            
            // 手动分页
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, allBatchIds.size());
            List<String> pagedBatchIds = allBatchIds.subList(start, end);
            
            List<Map<String, Object>> users = new ArrayList<>();
            
            for (String batchId : pagedBatchIds) {
                // 获取该批次的所有文件
                List<ProofFile> batchFiles = proofFileRepository.findBySubmissionBatchIdOrderByUploadTimeAsc(batchId);
                if (batchFiles.isEmpty()) continue;
                
                // 使用第一个文件的信息作为批次代表
                ProofFile firstFile = batchFiles.get(0);
                
                // 获取用户信息
                UserAccount user = userAccountService.findById(firstFile.getUserAccountId());
                if (user == null) continue;
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", firstFile.getId());  // 使用第一个文件的ID
                userInfo.put("batchId", batchId);  // 添加批次ID
                userInfo.put("fileCount", batchFiles.size());  // 添加文件数量
                userInfo.put("username", user.getDisplayName() != null ? user.getDisplayName() : user.getWalletAddress());
                userInfo.put("email", user.getWalletAddress());
                userInfo.put("walletAddress", user.getWalletAddress());
                userInfo.put("originalFilename", firstFile.getOriginalName());
                userInfo.put("fileName", firstFile.getFileName());
                userInfo.put("fileSize", firstFile.getFileSize());
                userInfo.put("uploadTime", firstFile.getUploadTime().toString());
                userInfo.put("auditStatus", firstFile.getAuditStatus().getDescription());
                userInfo.put("auditStatusCode", firstFile.getAuditStatus().name()); // 添加状态码用于前端判断
                userInfo.put("auditTime", firstFile.getAuditTime() != null ? firstFile.getAuditTime().toString() : null);
                userInfo.put("medalAwarded", firstFile.getMedalAwarded() != null ? firstFile.getMedalAwarded().name() : null);
                userInfo.put("objectKey", firstFile.getFileName());
                userInfo.put("filePath", firstFile.getFilePath());
                userInfo.put("userAccountId", firstFile.getUserAccountId());
                
                users.add(userInfo);
            }
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) allBatchIds.size() / limit);
            
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "获取成功");
            response.put("users", users);
            response.put("total", allBatchIds.size());  // 总批次数
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            
            logger.info("成功获取{}个已审核批次", users.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取已审核文件列表失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取所有文件列表（待审核+已审核+已拒绝，按批次分组）
     */
    @GetMapping("/all-users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取所有批次列表: page={}, limit={}", page, limit);
            
            // 直接查询所有批次ID（不区分状态，按时间倒序）
            List<String> allBatchIds = proofFileRepository.findAllDistinctBatchIds();
            
            logger.info("找到 {} 个批次（按时间倒序）", allBatchIds.size());
            
            // 手动分页
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, allBatchIds.size());
            List<String> pagedBatchIds = allBatchIds.subList(start, end);
            
            List<Map<String, Object>> users = new ArrayList<>();
            
            for (String batchId : pagedBatchIds) {
                // 获取该批次的所有文件
                List<ProofFile> batchFiles = proofFileRepository.findBySubmissionBatchIdOrderByUploadTimeAsc(batchId);
                if (batchFiles.isEmpty()) continue;
                
                // 使用第一个文件的信息作为批次代表
                ProofFile firstFile = batchFiles.get(0);
                
                // 获取用户信息
                UserAccount user = userAccountService.findById(firstFile.getUserAccountId());
                if (user == null) continue;
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", firstFile.getId());  // 使用第一个文件的ID
                userInfo.put("batchId", batchId);  // 添加批次ID
                userInfo.put("fileCount", batchFiles.size());  // 添加文件数量
                userInfo.put("username", user.getDisplayName() != null ? user.getDisplayName() : user.getWalletAddress());
                userInfo.put("email", user.getWalletAddress());
                userInfo.put("walletAddress", user.getWalletAddress());
                userInfo.put("originalFilename", firstFile.getOriginalName());
                userInfo.put("fileName", firstFile.getFileName());
                userInfo.put("fileSize", firstFile.getFileSize());
                userInfo.put("uploadTime", firstFile.getUploadTime().toString());
                userInfo.put("auditStatus", firstFile.getAuditStatus().getDescription());
                userInfo.put("auditStatusCode", firstFile.getAuditStatus().name()); // 添加状态码用于前端判断
                userInfo.put("auditTime", firstFile.getAuditTime() != null ? firstFile.getAuditTime().toString() : null);
                userInfo.put("medalAwarded", firstFile.getMedalAwarded() != null ? firstFile.getMedalAwarded().name() : null);
                userInfo.put("objectKey", firstFile.getFileName());
                userInfo.put("filePath", firstFile.getFilePath());
                userInfo.put("userAccountId", firstFile.getUserAccountId());
                
                users.add(userInfo);
            }
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) allBatchIds.size() / limit);
            
            response.put("code", 1);
            response.put("success", true);
            response.put("message", "获取成功");
            response.put("users", users);
            response.put("total", allBatchIds.size());  // 总批次数
            response.put("currentPage", page);
            response.put("totalPages", totalPages);
            
            logger.info("成功获取{}个批次", users.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取文件列表失败", e);
            response.put("code", 0);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 审核文件并分配勋章
     */
    @PostMapping("/review")
    public ResponseEntity<Map<String, Object>> reviewUser(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = (String) request.get("username");
            Integer approve = (Integer) request.get("approve");
            Integer goldNum = (Integer) request.get("firstnum");
            Integer silverNum = (Integer) request.get("secondnum");
            Integer bronzeNum = (Integer) request.get("thirdnum");
            Long proofFileId = request.get("proofFileId") != null ? 
                Long.valueOf(request.get("proofFileId").toString()) : null;
            
            logger.info("审核用户: username={}, approve={}, gold={}, silver={}, bronze={}, proofFileId={}", 
                username, approve, goldNum, silverNum, bronzeNum, proofFileId);
            
            if (username == null || approve == null) {
                response.put("success", false);
                response.put("message", "缺少必要参数");
                return ResponseEntity.status(400).body(response);
            }
            
            // 查找用户
            UserAccount user = userAccountService.findByDisplayNameOrWalletAddress(username);
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在: " + username);
                return ResponseEntity.status(400).body(response);
            }
            
            if (approve == 1) {
                // 审核通过，分配勋章
                if (goldNum != null && goldNum > 0) {
                    user.setGoldMedals(user.getGoldMedals() + goldNum);
                }
                if (silverNum != null && silverNum > 0) {
                    user.setSilverMedals(user.getSilverMedals() + silverNum);
                }
                if (bronzeNum != null && bronzeNum > 0) {
                    user.setBronzeMedals(user.getBronzeMedals() + bronzeNum);
                }
                
                // 更新用户信息
                user.setUpdateTime(LocalDateTime.now());
                userAccountService.save(user);
                
                // 调用区块链发放勋章
                try {
                    logger.info("开始向区块链发放勋章: 用户={}, 金牌={}, 银牌={}, 铜牌={}", 
                        user.getWalletAddress(), goldNum, silverNum, bronzeNum);
                    
                    DistributeRequest distributeRequest = new DistributeRequest();
                    distributeRequest.setTo(user.getWalletAddress());
                    distributeRequest.setGoldQty(goldNum != null ? goldNum : 0);
                    distributeRequest.setSilverQty(silverNum != null ? silverNum : 0);
                    distributeRequest.setBronzeQty(bronzeNum != null ? bronzeNum : 0);
                    
                    DistributeResponse distributeResponse = blockchainService.distributeMedalsWithWalletSigning(distributeRequest);
                    
                    if (distributeResponse.isSuccess()) {
                        logger.info("区块链勋章发放成功: txHash={}", distributeResponse.getTransactionHash());
                        
                        // 同步区块链数据回数据库
                        try {
                            blockchainSyncService.syncUserMedals(user.getWalletAddress());
                            logger.info("区块链数据同步成功");
                        } catch (Exception syncEx) {
                            logger.error("区块链数据同步失败: {}", syncEx.getMessage());
                        }
                    } else {
                        logger.error("区块链勋章发放失败: {}", distributeResponse.getMessage());
                    }
                } catch (Exception blockchainEx) {
                    logger.error("区块链操作失败: {}", blockchainEx.getMessage(), blockchainEx);
                    // 区块链操作失败不影响审核流程，只记录日志
                }
                
                // 更新相关证明文件的审核状态
                if (proofFileId != null) {
                    // 审核特定文件及其批次中的所有文件
                    Optional<ProofFile> proofFileOpt = proofFileRepository.findById(proofFileId);
                    if (proofFileOpt.isPresent()) {
                        ProofFile proofFile = proofFileOpt.get();
                        if (proofFile.getUserAccountId().equals(user.getId()) && 
                            proofFile.getAuditStatus() == ProofFile.AuditStatus.PENDING) {
                            
                            // 获取该批次的所有文件
                            List<ProofFile> batchFiles = new ArrayList<>();
                            if (proofFile.getSubmissionBatchId() != null) {
                                batchFiles = proofFileRepository.findBySubmissionBatchIdOrderByUploadTimeAsc(
                                    proofFile.getSubmissionBatchId());
                                logger.info("审核批次 {}, 包含 {} 个文件", proofFile.getSubmissionBatchId(), batchFiles.size());
                            } else {
                                // 兼容旧数据
                                batchFiles.add(proofFile);
                            }
                            
                            // 更新批次中所有文件的审核状态
                            for (ProofFile file : batchFiles) {
                                file.setAuditStatus(ProofFile.AuditStatus.APPROVED);
                                file.setAuditTime(LocalDateTime.now());
                                
                                // 设置主要勋章类型（选择数量最多的）
                                if (goldNum != null && goldNum > 0) {
                                    file.setMedalAwarded(ProofFile.MedalType.GOLD);
                                } else if (silverNum != null && silverNum > 0) {
                                    file.setMedalAwarded(ProofFile.MedalType.SILVER);
                                } else if (bronzeNum != null && bronzeNum > 0) {
                                    file.setMedalAwarded(ProofFile.MedalType.BRONZE);
                                }
                                
                                file.setMedalAwardTime(LocalDateTime.now());
                                proofFileRepository.save(file);
                            }
                            
                            logger.info("审核通过: 批次中的 {} 个文件已全部审核通过", batchFiles.size());
                        } else {
                            logger.warn("文件ID={}不属于用户{}或已审核", proofFileId, username);
                        }
                    } else {
                        logger.warn("未找到文件ID={}", proofFileId);
                    }
                } else {
                    // 兼容旧逻辑：审核该用户所有待审核文件
                    List<ProofFile> userProofFiles = proofFileRepository.findByUserAccountIdAndAuditStatus(
                        user.getId(), ProofFile.AuditStatus.PENDING);
                    
                    for (ProofFile proofFile : userProofFiles) {
                        proofFile.setAuditStatus(ProofFile.AuditStatus.APPROVED);
                        proofFile.setAuditTime(LocalDateTime.now());
                        
                        // 设置主要勋章类型（选择数量最多的）
                        if (goldNum != null && goldNum > 0) {
                            proofFile.setMedalAwarded(ProofFile.MedalType.GOLD);
                        } else if (silverNum != null && silverNum > 0) {
                            proofFile.setMedalAwarded(ProofFile.MedalType.SILVER);
                        } else if (bronzeNum != null && bronzeNum > 0) {
                            proofFile.setMedalAwarded(ProofFile.MedalType.BRONZE);
                        }
                        
                        proofFile.setMedalAwardTime(LocalDateTime.now());
                        proofFileRepository.save(proofFile);
                    }
                }
                
                response.put("success", true);
                response.put("code", 1);
                response.put("message", String.format("审核通过！为用户 %s 分配了 %d 金牌、%d 银牌、%d 铜牌", 
                    username, goldNum != null ? goldNum : 0, silverNum != null ? silverNum : 0, bronzeNum != null ? bronzeNum : 0));
                
                logger.info("审核成功: 用户{}获得金牌{}、银牌{}、铜牌{}", username, goldNum, silverNum, bronzeNum);
                
            } else {
                // 审核拒绝
                if (proofFileId != null) {
                    // 拒绝特定文件及其批次中的所有文件
                    Optional<ProofFile> proofFileOpt = proofFileRepository.findById(proofFileId);
                    if (proofFileOpt.isPresent()) {
                        ProofFile proofFile = proofFileOpt.get();
                        if (proofFile.getUserAccountId().equals(user.getId()) && 
                            proofFile.getAuditStatus() == ProofFile.AuditStatus.PENDING) {
                            
                            // 获取该批次的所有文件
                            List<ProofFile> batchFiles = new ArrayList<>();
                            if (proofFile.getSubmissionBatchId() != null) {
                                batchFiles = proofFileRepository.findBySubmissionBatchIdOrderByUploadTimeAsc(
                                    proofFile.getSubmissionBatchId());
                                logger.info("拒绝批次 {}, 包含 {} 个文件", proofFile.getSubmissionBatchId(), batchFiles.size());
                            } else {
                                // 兼容旧数据
                                batchFiles.add(proofFile);
                            }
                            
                            // 更新批次中所有文件的审核状态
                            for (ProofFile file : batchFiles) {
                                file.setAuditStatus(ProofFile.AuditStatus.REJECTED);
                                file.setAuditTime(LocalDateTime.now());
                                proofFileRepository.save(file);
                            }
                            
                            logger.info("审核拒绝: 批次中的 {} 个文件已全部拒绝", batchFiles.size());
                        } else {
                            logger.warn("文件ID={}不属于用户{}或已审核", proofFileId, username);
                        }
                    } else {
                        logger.warn("未找到文件ID={}", proofFileId);
                    }
                } else {
                    // 兼容旧逻辑：拒绝该用户所有待审核文件
                    List<ProofFile> userProofFiles = proofFileRepository.findByUserAccountIdAndAuditStatus(
                        user.getId(), ProofFile.AuditStatus.PENDING);
                    
                    for (ProofFile proofFile : userProofFiles) {
                        proofFile.setAuditStatus(ProofFile.AuditStatus.REJECTED);
                        proofFile.setAuditTime(LocalDateTime.now());
                        proofFileRepository.save(proofFile);
                    }
                }
                
                response.put("success", true);
                response.put("code", 1);
                response.put("message", "审核拒绝已处理");
                
                logger.info("审核拒绝: 用户{}", username);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("审核处理失败", e);
            response.put("success", false);
            response.put("message", "审核失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            logger.info("下载文件请求: {}", fileName);
            
            // 查找文件记录
            ProofFile proofFile = proofFileRepository.findFirstByFileName(fileName);
            if (proofFile == null) {
                logger.warn("文件不存在: {}", fileName);
                return ResponseEntity.notFound().build();
            }
            
            // 构建文件路径 - 修复路径问题
            String storedPath = proofFile.getFilePath();
            Path filePath;
            
            // 如果是绝对路径（以/开头），转换为相对路径
            if (storedPath.startsWith("/uploads/")) {
                filePath = Paths.get(storedPath.substring(1)); // 去掉开头的"/"
            } else {
                filePath = Paths.get(storedPath);
            }
            
            if (!Files.exists(filePath)) {
                logger.warn("物理文件不存在: {} (原始路径: {})", filePath, storedPath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件的 MIME 类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            logger.info("文件下载成功: {} ({})", fileName, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + proofFile.getOriginalName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("文件下载失败: " + fileName, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 获取单个材料的详细信息
     */
    @GetMapping("/material-detail/{id}")
    public ResponseEntity<Map<String, Object>> getMaterialDetail(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取材料详情: id={}", id);
            
            // 查找证明文件
            Optional<ProofFile> proofFileOpt = proofFileRepository.findById(id);
            if (!proofFileOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "材料不存在");
                return ResponseEntity.status(404).body(response);
            }
            ProofFile proofFile = proofFileOpt.get();
            
            // 获取用户信息
            UserAccount user = userAccountService.findById(proofFile.getUserAccountId());
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            // 获取该批次的所有证明文件
            List<ProofFile> batchFiles = new ArrayList<>();
            if (proofFile.getSubmissionBatchId() != null) {
                batchFiles = proofFileRepository.findBySubmissionBatchIdOrderByUploadTimeAsc(
                    proofFile.getSubmissionBatchId());
                logger.info("该提交包含 {} 个证明文件", batchFiles.size());
            } else {
                // 兼容旧数据（没有批次ID）
                batchFiles.add(proofFile);
            }
            
            // 构建证明文件列表
            List<Map<String, Object>> proofFilesList = new ArrayList<>();
            for (ProofFile file : batchFiles) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", file.getId());
                fileInfo.put("fileName", file.getFileName());
                fileInfo.put("originalFilename", file.getOriginalName());
                fileInfo.put("fileType", file.getFileType());
                fileInfo.put("fileSize", file.getFileSize());
                fileInfo.put("filePath", file.getFilePath());
                fileInfo.put("objectKey", file.getFileName());
                fileInfo.put("downloadUrl", "http://localhost:5000/api/admin/download/" + file.getFileName());
                proofFilesList.add(fileInfo);
            }
            
            // 查找关联的NFT图片（如果有）
            List<com.brokerwallet.entity.NftImage> userNftImages = nftImageRepository.findByUserAccountIdOrderByUploadTimeDesc(user.getId());
            Map<String, Object> nftImageInfo = null;
            if (!userNftImages.isEmpty()) {
                // 取最新上传的NFT图片
                com.brokerwallet.entity.NftImage nftImage = userNftImages.get(0);
                nftImageInfo = new HashMap<>();
                nftImageInfo.put("id", nftImage.getId());
                nftImageInfo.put("originalName", nftImage.getOriginalName());
                nftImageInfo.put("imageName", nftImage.getImageName());
                nftImageInfo.put("imageType", nftImage.getImageType());
                nftImageInfo.put("imageSize", nftImage.getImageSize());
                nftImageInfo.put("imagePath", nftImage.getImagePath());
                nftImageInfo.put("thumbnailPath", nftImage.getThumbnailPath());
                // 构建预览URL（完整URL）
                nftImageInfo.put("previewUrl", "http://localhost:5000/api/admin/nft-image/" + nftImage.getImageName());
                nftImageInfo.put("thumbnailUrl", "http://localhost:5000/api/admin/nft-thumbnail/" + nftImage.getImageName());
                nftImageInfo.put("uploadTime", nftImage.getUploadTime().toString());
                nftImageInfo.put("mintStatus", nftImage.getMintStatus().name());
                
                // ✅ 关键修复：读取并返回Base64编码的图片数据
                try {
                    String imagePath = nftImage.getImagePath();
                    Path imageFilePath;
                    
                    // 处理路径（与下载文件逻辑一致）
                    if (imagePath.startsWith("/uploads/")) {
                        imageFilePath = Paths.get(imagePath.substring(1));
                    } else {
                        imageFilePath = Paths.get(imagePath);
                    }
                    
                    if (Files.exists(imageFilePath)) {
                        byte[] imageBytes = Files.readAllBytes(imageFilePath);
                        String base64Data = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        // 根据图片类型添加Data URL前缀
                        String mimeType = nftImage.getImageType() != null ? nftImage.getImageType() : "image/jpeg";
                        String imageData = "data:" + mimeType + ";base64," + base64Data;
                        nftImageInfo.put("imageData", imageData);
                        logger.info("成功读取NFT图片数据，大小: {} bytes", imageBytes.length);
                    } else {
                        logger.warn("NFT图片文件不存在: {}", imageFilePath);
                        nftImageInfo.put("imageData", null);
                    }
                } catch (Exception e) {
                    logger.error("读取NFT图片失败", e);
                    nftImageInfo.put("imageData", null);
                }
            }
            
            Map<String, Object> materialDetail = new HashMap<>();
            materialDetail.put("id", proofFile.getId());
            materialDetail.put("fileName", proofFile.getFileName());
            materialDetail.put("originalFilename", proofFile.getOriginalName());
            materialDetail.put("fileType", proofFile.getFileType());
            materialDetail.put("fileSize", proofFile.getFileSize());
            materialDetail.put("filePath", proofFile.getFilePath());
            materialDetail.put("uploadTime", proofFile.getUploadTime().toString());
            materialDetail.put("auditStatus", proofFile.getAuditStatus().getDescription());
            materialDetail.put("auditStatusCode", proofFile.getAuditStatus().name()); // 添加状态码（PENDING/APPROVED/REJECTED）
            materialDetail.put("objectKey", proofFile.getFileName());
            
            // 审核详细信息
            if (proofFile.getAuditTime() != null) {
                materialDetail.put("auditTime", proofFile.getAuditTime().toString());
            }
            if (proofFile.getMedalAwarded() != null) {
                materialDetail.put("medalAwarded", proofFile.getMedalAwarded().name());
                materialDetail.put("medalAwardedDesc", proofFile.getMedalAwarded().getDescription());
            }
            if (proofFile.getMedalAwardTime() != null) {
                materialDetail.put("medalAwardTime", proofFile.getMedalAwardTime().toString());
            }
            // Token转账金额（如果有）
            if (proofFile.getTokenReward() != null) {
                // 转换为wei（BigDecimal已经是wei单位）
                materialDetail.put("tokenAmount", proofFile.getTokenReward().toString());
            }
            // Token转账交易哈希
            if (proofFile.getTokenRewardTxHash() != null) {
                materialDetail.put("tokenTransferTxHash", proofFile.getTokenRewardTxHash());
            }
            
            // 用户信息
            materialDetail.put("userAccountId", user.getId());  // ⭐ 添加用户账户ID
            materialDetail.put("walletAddress", user.getWalletAddress());
            materialDetail.put("displayName", user.getDisplayName());
            materialDetail.put("username", user.getDisplayName() != null ? user.getDisplayName() : user.getWalletAddress());
            materialDetail.put("representativeWork", user.getRepresentativeWork());
            materialDetail.put("showRepresentativeWork", user.getShowRepresentativeWork());
            materialDetail.put("adminApprovedDisplay", user.getAdminApprovedDisplay());
            
            // 勋章信息
            materialDetail.put("goldMedals", user.getGoldMedals());
            materialDetail.put("silverMedals", user.getSilverMedals());
            materialDetail.put("bronzeMedals", user.getBronzeMedals());
            
            // NFT图片信息
            materialDetail.put("nftImage", nftImageInfo);
            
            // 批次信息
            materialDetail.put("submissionBatchId", proofFile.getSubmissionBatchId());
            materialDetail.put("proofFiles", proofFilesList);  // 该批次的所有证明文件
            materialDetail.put("proofFileCount", batchFiles.size());  // 证明文件数量
            
            response.put("success", true);
            response.put("data", materialDetail);
            
            logger.info("材料详情获取成功: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取材料详情失败", e);
            response.put("success", false);
            response.put("message", "获取详情失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 更新用户代表作展示审批状态
     */
    @PostMapping("/approve-representative-work")
    public ResponseEntity<Map<String, Object>> approveRepresentativeWork(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("📥 收到代表作审批请求: {}", request);
        
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            Boolean approved = (Boolean) request.get("approved");
            
            logger.info("🔍 更新代表作展示审批: userId={}, approved={}", userId, approved);
            
            UserAccount user = userAccountService.findById(userId);
            if (user == null) {
                logger.error("❌ 用户不存在: userId={}", userId);
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(400).body(response);
            }
            
            logger.info("📝 更新前: wallet={}, displayName={}, adminApprovedDisplay={}", 
                    user.getWalletAddress(), user.getDisplayName(), user.getAdminApprovedDisplay());
            
            user.setAdminApprovedDisplay(approved);
            user.setUpdateTime(LocalDateTime.now());
            userAccountService.save(user);
            
            logger.info("✅ 更新后: wallet={}, displayName={}, adminApprovedDisplay={}", 
                    user.getWalletAddress(), user.getDisplayName(), user.getAdminApprovedDisplay());
            
            response.put("success", true);
            response.put("message", approved ? "已同意展示代表作" : "已拒绝展示代表作");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新代表作展示审批失败", e);
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查用户勋章数据一致性
     */
    @GetMapping("/check-medals")
    public ResponseEntity<Map<String, Object>> checkMedals() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("检查所有用户勋章数据");
            
            List<UserAccount> allUsers = userAccountRepository.findAll();
            List<Map<String, Object>> userMedals = new ArrayList<>();
            
            for (UserAccount user : allUsers) {
                // 只显示有勋章的用户
                if (user.getGoldMedals() > 0 || user.getSilverMedals() > 0 || user.getBronzeMedals() > 0 ||
                    user.getBlockchainGoldMedals() > 0 || user.getBlockchainSilverMedals() > 0 || user.getBlockchainBronzeMedals() > 0) {
                    
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("walletAddress", user.getWalletAddress());
                    userInfo.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "未设置");
                    
                    // 数据库中的勋章数据
                    Map<String, Integer> dbMedals = new HashMap<>();
                    dbMedals.put("gold", user.getGoldMedals());
                    dbMedals.put("silver", user.getSilverMedals());
                    dbMedals.put("bronze", user.getBronzeMedals());
                    dbMedals.put("total", user.getTotalMedals());
                    userInfo.put("databaseMedals", dbMedals);
                    
                    // 区块链同步的勋章数据
                    Map<String, Integer> bcMedals = new HashMap<>();
                    bcMedals.put("gold", user.getBlockchainGoldMedals());
                    bcMedals.put("silver", user.getBlockchainSilverMedals());
                    bcMedals.put("bronze", user.getBlockchainBronzeMedals());
                    userInfo.put("blockchainMedals", bcMedals);
                    
                    // 一致性检查
                    boolean isConsistent = user.getGoldMedals().equals(user.getBlockchainGoldMedals()) &&
                                          user.getSilverMedals().equals(user.getBlockchainSilverMedals()) &&
                                          user.getBronzeMedals().equals(user.getBlockchainBronzeMedals());
                    userInfo.put("isConsistent", isConsistent);
                    userInfo.put("consistencyStatus", isConsistent ? "✅ 一致" : "❌ 不一致");
                    
                    userInfo.put("blockchainSyncTime", user.getBlockchainSyncTime() != null ? 
                        user.getBlockchainSyncTime().toString() : "未同步");
                    
                    userMedals.add(userInfo);
                }
            }
            
            response.put("success", true);
            response.put("data", userMedals);
            response.put("totalUsers", userMedals.size());
            
            logger.info("成功检查{}个用户的勋章数据", userMedals.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查勋章数据失败", e);
            response.put("success", false);
            response.put("message", "检查失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取所有证明文件（用于调试）
     */
    @GetMapping("/all-files")
    public ResponseEntity<Map<String, Object>> getAllFiles() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("获取所有证明文件");
            
            List<ProofFile> allFiles = proofFileRepository.findAll();
            List<Map<String, Object>> fileList = new ArrayList<>();
            
            for (ProofFile file : allFiles) {
                UserAccount user = userAccountService.findById(file.getUserAccountId());
                
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", file.getId());
                fileInfo.put("fileName", file.getFileName());
                fileInfo.put("originalName", file.getOriginalName());
                fileInfo.put("auditStatus", file.getAuditStatus().name()); // 显示枚举名称
                fileInfo.put("auditStatusDesc", file.getAuditStatus().getDescription()); // 显示描述
                fileInfo.put("fileStatus", file.getStatus().name()); // 显示枚举名称
                fileInfo.put("fileStatusDesc", file.getStatus().getDescription()); // 显示描述
                fileInfo.put("uploadTime", file.getUploadTime().toString());
                fileInfo.put("userAccountId", file.getUserAccountId());
                fileInfo.put("fileSize", file.getFileSize());
                fileInfo.put("filePath", file.getFilePath());
                
                if (user != null) {
                    fileInfo.put("userWallet", user.getWalletAddress());
                    fileInfo.put("userDisplayName", user.getDisplayName());
                } else {
                    fileInfo.put("userWallet", "用户不存在");
                    fileInfo.put("userDisplayName", "用户不存在");
                }
                
                fileList.add(fileInfo);
            }
            
            response.put("success", true);
            response.put("data", fileList);
            response.put("total", allFiles.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取所有文件失败", e);
            response.put("success", false);
            response.put("message", "获取失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 修复用户6的证明文件（如果物理文件存在）
     */
    @PostMapping("/restore-user6-files")
    public ResponseEntity<Map<String, Object>> restoreUser6Files() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("尝试恢复用户6的证明文件");
            
            // 查找用户6
            UserAccount user6 = userAccountService.findByWalletAddress("8c056ccb92c567da3fee27c23d4f2f107f203879");
            if (user6 == null) {
                response.put("success", false);
                response.put("message", "用户6不存在");
                return ResponseEntity.status(400).body(response);
            }
            
            // 检查uploads目录中是否有用户6的文件
            String userProofDir = "/uploads/proofs/users/6/";
            String userNftDir = "/uploads/nft-images/users/6/";
            
            // 这里需要扫描实际的文件系统
            // 暂时创建一些基于现有物理文件的记录
            
            // 根据你提到的，应该有4个文件，让我创建对应的数据库记录
            String[] existingFiles = {
                "1da9606195df43cb97b50196071ffec4_1758201127485.docx",
                "c8f2b013e12942428da9362d3b1c3b0d_1758189455863.jpg", 
                "fee37200f25347649b7a46361e74b51b_1758201127547.jpg",
                "783041a98a31457ca6fe279833a15312_1758182975497"
            };
            
            int createdCount = 0;
            for (String fileName : existingFiles) {
                // 检查数据库中是否已存在
                ProofFile existingFile = proofFileRepository.findFirstByFileName(fileName);
                if (existingFile == null) {
                    ProofFile proofFile = new ProofFile();
                    proofFile.setUserAccountId(user6.getId());
                    proofFile.setFileName(fileName);
                    proofFile.setOriginalName(fileName);
                    proofFile.setFileType(fileName.endsWith(".docx") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : 
                                         fileName.endsWith(".jpg") ? "image/jpeg" : "application/octet-stream");
                    proofFile.setFileSize(1000000L); // 默认1MB
                    proofFile.setFilePath(userProofDir + fileName);
                    proofFile.setAuditStatus(ProofFile.AuditStatus.PENDING);
                    proofFile.setUploadTime(LocalDateTime.now().minusDays(1)); // 1天前上传
                    
                    proofFileRepository.save(proofFile);
                    createdCount++;
                    logger.info("恢复文件记录: {}", fileName);
                }
            }
            
            response.put("success", true);
            response.put("message", "恢复了 " + createdCount + " 个文件记录");
            response.put("data", Map.of("restoredFiles", createdCount, "userId", user6.getId()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("恢复用户6文件失败", e);
            response.put("success", false);
            response.put("message", "恢复失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 彻底清理所有虚假测试用户，只保留真实用户6
     */
    @DeleteMapping("/clean-all-fake-users")
    public ResponseEntity<Map<String, Object>> cleanAllFakeUsers() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始彻底清理所有虚假测试用户");
            
            // 真实用户6的钱包地址（要保留的）
            String realUserWallet = "8c056ccb92c567da3fee27c23d4f2f107f203879";
            
            // 获取所有用户
            List<UserAccount> allUsers = userAccountRepository.findAll();
            int deletedFileCount = 0;
            int deletedUserCount = 0;
            
            for (UserAccount user : allUsers) {
                // 如果不是真实用户6，就删除
                if (!realUserWallet.equals(user.getWalletAddress())) {
                    logger.info("准备删除虚假用户: {} - {} (ID: {})", 
                        user.getDisplayName(), user.getWalletAddress(), user.getId());
                    
                    // 先删除该用户的所有证明文件
                    List<ProofFile> userFiles = proofFileRepository.findByUserAccountIdOrderByUploadTimeDesc(user.getId());
                    for (ProofFile file : userFiles) {
                        logger.info("删除虚假文件: {} (ID: {})", file.getFileName(), file.getId());
                        proofFileRepository.delete(file);
                        deletedFileCount++;
                    }
                    
                    // 删除用户账户
                    userAccountRepository.delete(user);
                    deletedUserCount++;
                    logger.info("删除虚假用户: {} (ID: {})", user.getWalletAddress(), user.getId());
                } else {
                    logger.info("保留真实用户6: {} - {} (ID: {})", 
                        user.getDisplayName(), user.getWalletAddress(), user.getId());
                }
            }
            
            response.put("success", true);
            response.put("message", String.format("彻底清理完成！删除了 %d 个虚假用户和 %d 个虚假文件，保留了真实用户6", 
                deletedUserCount, deletedFileCount));
            response.put("data", Map.of(
                "deletedUsers", deletedUserCount,
                "deletedFiles", deletedFileCount,
                "preservedUser", realUserWallet
            ));
            
            logger.info("彻底清理完成，删除了{}个虚假用户和{}个虚假文件", deletedUserCount, deletedFileCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("彻底清理失败", e);
            response.put("success", false);
            response.put("message", "清理失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据实际文件重新同步用户6的数据库记录
     */
    @PostMapping("/sync-user6-files")
    public ResponseEntity<Map<String, Object>> syncUser6Files() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始同步用户6的真实文件数据");
            
            // 查找用户6
            UserAccount user6 = userAccountService.findByWalletAddress("8c056ccb92c567da3fee27c23d4f2f107f203879");
            if (user6 == null) {
                response.put("success", false);
                response.put("message", "用户6不存在");
                return ResponseEntity.status(400).body(response);
            }
            
            // 先删除所有旧的记录
            List<ProofFile> oldFiles = proofFileRepository.findByUserAccountIdOrderByUploadTimeDesc(user6.getId());
            for (ProofFile oldFile : oldFiles) {
                proofFileRepository.delete(oldFile);
            }
            logger.info("删除了{}个旧记录", oldFiles.size());
            
            // 根据实际文件创建新记录（使用真实的文件信息）
            Map<String, Object>[] realFiles = new Map[]{
                Map.of("fileName", "1bd51835e1cc4cebaf6d3d517e13fb7b_1758189455827.docx", 
                       "size", 17474L, "type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                       "time", "2025-09-18T17:57:35"),
                Map.of("fileName", "1da9606195df43cb97b50196071ffec4_1758201127485.docx", 
                       "size", 1002771L, "type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                       "time", "2025-09-18T21:12:07"),
                Map.of("fileName", "79b1a60c1e834deca9bca40f41ca2b96_1758189455794.docx", 
                       "size", 12807L, "type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                       "time", "2025-09-18T17:57:35"),
                Map.of("fileName", "a6ab9f5751d3408a931e23f94f8dc107_1758182975497", 
                       "size", 455683L, "type", "application/octet-stream",
                       "time", "2025-09-18T16:09:35")
            };
            
            int createdCount = 0;
            for (Map<String, Object> fileInfo : realFiles) {
                ProofFile proofFile = new ProofFile();
                proofFile.setUserAccountId(user6.getId());
                proofFile.setFileName((String) fileInfo.get("fileName"));
                proofFile.setOriginalName((String) fileInfo.get("fileName"));
                proofFile.setFileType((String) fileInfo.get("type"));
                proofFile.setFileSize((Long) fileInfo.get("size"));
                proofFile.setFilePath("/uploads/proofs/users/6/" + fileInfo.get("fileName"));
                proofFile.setAuditStatus(ProofFile.AuditStatus.PENDING);
                proofFile.setUploadTime(LocalDateTime.parse((String) fileInfo.get("time")));
                
                proofFileRepository.save(proofFile);
                createdCount++;
                logger.info("创建真实文件记录: {} (大小: {})", fileInfo.get("fileName"), fileInfo.get("size"));
            }
            
            response.put("success", true);
            response.put("message", "同步完成！重新创建了 " + createdCount + " 个真实文件记录");
            response.put("data", Map.of("syncedFiles", createdCount, "userId", user6.getId()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("同步用户6文件失败", e);
            response.put("success", false);
            response.put("message", "同步失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 重置用户6的审核状态和勋章
     */
    @PostMapping("/reset-user6-audit")
    public ResponseEntity<Map<String, Object>> resetUser6Audit() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始重置用户6的审核状态");
            
            // 查找用户6
            UserAccount user6 = userAccountService.findByWalletAddress("8c056ccb92c567da3fee27c23d4f2f107f203879");
            if (user6 == null) {
                response.put("success", false);
                response.put("message", "用户6不存在");
                return ResponseEntity.status(400).body(response);
            }
            
            // 重置用户勋章为0
            user6.setGoldMedals(0);
            user6.setSilverMedals(0);
            user6.setBronzeMedals(0);
            user6.setUpdateTime(LocalDateTime.now());
            userAccountService.save(user6);
            logger.info("重置用户6的勋章数量");
            
            // 将所有文件状态重置为PENDING
            List<ProofFile> userFiles = proofFileRepository.findByUserAccountIdOrderByUploadTimeDesc(user6.getId());
            int resetCount = 0;
            
            for (ProofFile file : userFiles) {
                file.setAuditStatus(ProofFile.AuditStatus.PENDING);
                file.setAuditTime(null);
                file.setMedalAwarded(ProofFile.MedalType.NONE);
                file.setMedalAwardTime(null);
                file.setMedalTransactionHash(null);
                proofFileRepository.save(file);
                resetCount++;
                logger.info("重置文件审核状态: {} (ID: {})", file.getFileName(), file.getId());
            }
            
            response.put("success", true);
            response.put("message", "重置完成！" + resetCount + " 个文件重新设为待审核状态，用户勋章已清零");
            response.put("data", Map.of(
                "resetFiles", resetCount,
                "userId", user6.getId(),
                "userWallet", user6.getWalletAddress()
            ));
            
            logger.info("用户6审核状态重置完成，重置了{}个文件", resetCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("重置用户6审核状态失败", e);
            response.put("success", false);
            response.put("message", "重置失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 提供NFT图片访问
     */
    @GetMapping("/nft-image/{imageName}")
    public ResponseEntity<Resource> getNftImage(@PathVariable String imageName) {
        try {
            logger.info("获取NFT图片: {}", imageName);
            
            // 查找NFT图片记录
            Optional<com.brokerwallet.entity.NftImage> nftImageOpt = nftImageRepository.findByImageName(imageName);
            if (!nftImageOpt.isPresent()) {
                logger.warn("NFT图片不存在: {}", imageName);
                return ResponseEntity.notFound().build();
            }
            
            com.brokerwallet.entity.NftImage nftImage = nftImageOpt.get();
            
            // 修复NFT图片路径问题
            String storedPath = nftImage.getImagePath();
            Path filePath;
            if (storedPath.startsWith("/uploads/")) {
                filePath = Paths.get(storedPath.substring(1)); // 去掉开头的"/"
            } else {
                filePath = Paths.get(storedPath);
            }
            
            if (!Files.exists(filePath)) {
                logger.warn("NFT图片文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件的 MIME 类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // 根据文件扩展名手动判断
                String fileName = filePath.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (fileName.endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    // 对于没有扩展名的文件，尝试作为图片处理
                    contentType = "image/jpeg"; // 默认作为JPEG处理
                }
            }
            
            logger.info("NFT图片访问成功: {} ({})", imageName, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // 1小时缓存
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("获取NFT图片失败: " + imageName, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 提供NFT缩略图访问
     */
    @GetMapping("/nft-thumbnail/{imageName}")
    public ResponseEntity<Resource> getNftThumbnail(@PathVariable String imageName) {
        try {
            logger.info("获取NFT缩略图: {}", imageName);
            
            // 查找NFT图片记录
            Optional<com.brokerwallet.entity.NftImage> nftImageOpt = nftImageRepository.findByImageName(imageName);
            if (!nftImageOpt.isPresent()) {
                logger.warn("NFT图片记录不存在: {}", imageName);
                return ResponseEntity.notFound().build();
            }
            
            com.brokerwallet.entity.NftImage nftImage = nftImageOpt.get();
            
            // 如果有缩略图路径，使用缩略图
            String thumbnailPath = nftImage.getThumbnailPath();
            String imagePath = nftImage.getImagePath();
            Path filePath;
            
            if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                // 修复缩略图路径
                if (thumbnailPath.startsWith("/uploads/")) {
                    filePath = Paths.get(thumbnailPath.substring(1));
                } else {
                    filePath = Paths.get(thumbnailPath);
                }
            } else {
                // 如果没有缩略图，使用原图
                if (imagePath.startsWith("/uploads/")) {
                    filePath = Paths.get(imagePath.substring(1));
                } else {
                    filePath = Paths.get(imagePath);
                }
            }
            
            if (!Files.exists(filePath)) {
                logger.warn("NFT缩略图文件不存在: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件的 MIME 类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                // 根据文件扩展名手动判断
                String fileName = filePath.getFileName().toString().toLowerCase();
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (fileName.endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    // 对于没有扩展名的文件，尝试作为图片处理
                    contentType = "image/jpeg"; // 默认作为JPEG处理
                }
            }
            
            logger.info("NFT缩略图访问成功: {} ({})", imageName, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // 1小时缓存
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("获取NFT缩略图失败: " + imageName, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 调试：检查NFT图片数据状态
     */
    @GetMapping("/debug/nft-images")
    public ResponseEntity<Map<String, Object>> debugNftImages() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("调试：检查NFT图片数据状态");
            
            // 获取所有NFT图片记录
            List<com.brokerwallet.entity.NftImage> allNftImages = nftImageRepository.findAll();
            
            // 获取用户6的NFT图片
            List<com.brokerwallet.entity.NftImage> user6NftImages = nftImageRepository.findByUserAccountIdOrderByUploadTimeDesc(6L);
            
            // 获取所有证明文件
            List<ProofFile> allProofFiles = proofFileRepository.findAll();
            
            List<Map<String, Object>> nftImageList = new ArrayList<>();
            for (com.brokerwallet.entity.NftImage nftImage : allNftImages) {
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("id", nftImage.getId());
                imageInfo.put("imageName", nftImage.getImageName());
                imageInfo.put("originalName", nftImage.getOriginalName());
                imageInfo.put("imagePath", nftImage.getImagePath());
                imageInfo.put("userAccountId", nftImage.getUserAccountId());
                imageInfo.put("proofFileId", nftImage.getProofFileId());
                imageInfo.put("uploadTime", nftImage.getUploadTime().toString());
                nftImageList.add(imageInfo);
            }
            
            List<Map<String, Object>> proofFileList = new ArrayList<>();
            for (ProofFile proofFile : allProofFiles) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", proofFile.getId());
                fileInfo.put("fileName", proofFile.getFileName());
                fileInfo.put("userAccountId", proofFile.getUserAccountId());
                fileInfo.put("uploadTime", proofFile.getUploadTime().toString());
                proofFileList.add(fileInfo);
            }
            
            response.put("success", true);
            response.put("data", Map.of(
                "totalNftImages", allNftImages.size(),
                "user6NftImages", user6NftImages.size(),
                "totalProofFiles", allProofFiles.size(),
                "nftImageList", nftImageList,
                "proofFileList", proofFileList
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("调试NFT图片数据失败", e);
            response.put("success", false);
            response.put("message", "调试失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 修复：根据文件系统同步NFT图片数据库记录
     */
    @PostMapping("/fix/sync-nft-images")
    public ResponseEntity<Map<String, Object>> fixSyncNftImages() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始修复：同步NFT图片数据库记录");
            
            // 根据实际文件系统中的文件创建数据库记录
            String[] nftImageFiles = {
                "783041a98a31457ca6fe279833a15312_1758182975497", // 无后缀
                "c8f2b013e12942428da9362d3b1c3b0d_1758189455863.jpg",
                "fee37200f25347649b7a46361e74b51b_1758201127547.jpg"
            };
            
            // 获取用户6的信息
            UserAccount user6 = userAccountService.findById(6L);
            if (user6 == null) {
                response.put("success", false);
                response.put("message", "用户6不存在");
                return ResponseEntity.status(400).body(response);
            }
            
            // 获取用户6的证明文件，按时间排序
            List<ProofFile> proofFiles = proofFileRepository.findByUserAccountIdOrderByUploadTimeDesc(6L);
            
            int createdCount = 0;
            for (int i = 0; i < nftImageFiles.length && i < proofFiles.size(); i++) {
                String fileName = nftImageFiles[i];
                ProofFile proofFile = proofFiles.get(i);
                
                // 检查是否已存在记录
                Optional<com.brokerwallet.entity.NftImage> existingImage = 
                    nftImageRepository.findByImageName(fileName);
                
                if (!existingImage.isPresent()) {
                    com.brokerwallet.entity.NftImage nftImage = new com.brokerwallet.entity.NftImage();
                    nftImage.setImageName(fileName);
                    nftImage.setOriginalName(fileName);
                    
                    // 根据文件扩展名确定类型
                    String imageType = "application/octet-stream";
                    if (fileName.endsWith(".jpg")) {
                        imageType = "image/jpeg";
                    }
                    nftImage.setImageType(imageType);
                    
                    // 设置文件大小（需要读取实际文件）
                    Path imagePath = Paths.get("uploads/nft-images/users/6/" + fileName);
                    long imageSize = 100000L; // 默认值
                    try {
                        if (Files.exists(imagePath)) {
                            imageSize = Files.size(imagePath);
                        }
                    } catch (Exception e) {
                        logger.warn("无法获取文件大小: {}", imagePath);
                    }
                    nftImage.setImageSize(imageSize);
                    
                    nftImage.setImagePath("uploads/nft-images/users/6/" + fileName);
                    nftImage.setThumbnailPath("uploads/thumbnails/users/6/thumb_" + fileName);
                    nftImage.setUserAccountId(user6.getId());
                    nftImage.setProofFileId(proofFile.getId()); // 关联到证明文件
                    nftImage.setUploadTime(proofFile.getUploadTime()); // 使用证明文件的上传时间
                    
                    nftImageRepository.save(nftImage);
                    createdCount++;
                    
                    logger.info("创建NFT图片记录: {} -> 证明文件ID: {}", fileName, proofFile.getId());
                }
            }
            
            response.put("success", true);
            response.put("message", "同步完成！创建了 " + createdCount + " 条NFT图片记录");
            response.put("data", Map.of(
                "createdRecords", createdCount,
                "totalFiles", nftImageFiles.length,
                "userId", user6.getId()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("修复NFT图片数据失败", e);
            response.put("success", false);
            response.put("message", "修复失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取审核统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long pendingCount = proofFileRepository.countByAuditStatus(ProofFile.AuditStatus.PENDING);
            long approvedCount = proofFileRepository.countByAuditStatus(ProofFile.AuditStatus.APPROVED);
            long rejectedCount = proofFileRepository.countByAuditStatus(ProofFile.AuditStatus.REJECTED);
            long totalCount = proofFileRepository.count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", pendingCount);
            stats.put("approved", approvedCount);
            stats.put("rejected", rejectedCount);
            stats.put("total", totalCount);
            
            response.put("success", true);
            response.put("data", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 按花名查询用户
     */
    @GetMapping("/search-by-display-name")
    public ResponseEntity<Map<String, Object>> searchByDisplayName(@RequestParam String displayName) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("按花名查询用户: {}", displayName);
        
        try {
            // 使用模糊查询
            List<UserAccount> users = userAccountRepository.findByDisplayNameContainingIgnoreCase(displayName);
            
            if (users.isEmpty()) {
                response.put("success", false);
                response.put("message", "未找到使用该花名的用户");
                return ResponseEntity.ok(response);
            }
            
            // 构建返回数据
            List<Map<String, Object>> userList = new ArrayList<>();
            for (UserAccount user : users) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("walletAddress", user.getWalletAddress());
                userInfo.put("displayName", user.getDisplayName());
                userInfo.put("goldMedals", user.getGoldMedals());
                userInfo.put("silverMedals", user.getSilverMedals());
                userInfo.put("bronzeMedals", user.getBronzeMedals());
                userInfo.put("totalMedals", user.getTotalMedals());
                userInfo.put("representativeWork", user.getRepresentativeWork());
                userInfo.put("showRepresentativeWork", user.getShowRepresentativeWork());
                userInfo.put("adminApprovedDisplay", user.getAdminApprovedDisplay());
                userList.add(userInfo);
            }
            
            response.put("success", true);
            response.put("count", users.size());
            response.put("users", userList);
            
            logger.info("找到 {} 个使用花名 '{}' 的用户", users.size(), displayName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("按花名查询失败", e);
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 查询管理员代币余额
     */
    @GetMapping("/check-token-balance")
    public ResponseEntity<Map<String, Object>> checkTokenBalance() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String adminAddress = blockchainService.getAccountAddress();
            java.math.BigInteger balance = blockchainService.getTokenBalance(adminAddress);
            java.math.BigInteger balanceInToken = balance.divide(java.math.BigInteger.TEN.pow(18));
            
            response.put("success", true);
            response.put("address", adminAddress);
            response.put("balanceWei", balance.toString());
            response.put("balanceToken", balanceInToken.toString());
            
            logger.info("管理员代币余额: {} Token ({} wei)", balanceInToken, balance);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询余额失败", e);
            response.put("success", false);
            response.put("message", "查询余额失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 管理员转账代币奖励
     */
    @PostMapping("/transfer-reward")
    public ResponseEntity<Map<String, Object>> transferReward(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("📥 收到转账奖励请求: {}", request);
        
        try {
            String toAddress = request.get("toAddress").toString();
            String amount = request.get("amount").toString();
            
            logger.info("🔍 转账奖励: 接收地址={}, 金额={} wei", toAddress, amount);
            
            // 调用区块链服务转账
            String txHash = blockchainService.transferTokenReward(toAddress, amount);
            
            logger.info("✅ 转账成功: txHash={}", txHash);
            
            response.put("success", true);
            response.put("message", "转账成功");
            response.put("transactionHash", txHash);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ 转账失败", e);
            response.put("success", false);
            response.put("message", "转账失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 生成默认样式NFT图片（使用contract项目的样式）
     */
    @PostMapping("/generate-default-nft-image")
    public ResponseEntity<Map<String, Object>> generateDefaultNftImage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("📥 收到生成默认NFT图片请求: {}", request);
        
        try {
            String authorInfo = request.get("authorInfo").toString();
            String eventType = request.get("eventType").toString();
            String eventDescription = request.get("eventDescription").toString();
            String contributionLevel = request.get("contributionLevel").toString();
            String timestamp = request.get("timestamp").toString();
            
            logger.info("生成默认NFT图片: 作者={}, 事件类型={}, 贡献等级={}", 
                authorInfo, eventType, contributionLevel);
            
            // 调用图片生成器
            String imageBase64 = medalImageGenerator.generateMedalImage(
                authorInfo, eventType, eventDescription, contributionLevel, timestamp
            );
            
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                logger.info("✅ 默认NFT图片生成成功，Base64长度: {}", imageBase64.length());
                
                // 添加data URL前缀，方便前端和手机端使用
                String dataUrl = "data:image/jpeg;base64," + imageBase64;
                
                response.put("success", true);
                response.put("message", "图片生成成功");
                response.put("imageData", dataUrl); // 返回完整的data URL
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "图片生成失败");
                return ResponseEntity.status(500).body(response);
            }
            
        } catch (Exception e) {
            logger.error("❌ 生成默认NFT图片失败", e);
            response.put("success", false);
            response.put("message", "生成失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 保存代币奖励到数据库
     */
    @PostMapping("/save-token-reward")
    public ResponseEntity<Map<String, Object>> saveTokenReward(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("📥 收到保存代币奖励请求: {}", request);
        
        try {
            Long proofFileId = Long.valueOf(request.get("proofFileId").toString());
            Number tokenRewardNum = (Number) request.get("tokenReward");
            String txHash = request.get("txHash").toString();
            
            // 转换为BigDecimal
            java.math.BigDecimal tokenReward = new java.math.BigDecimal(tokenRewardNum.toString());
            
            logger.info("保存代币奖励: proofFileId={}, tokenReward={} BKC, txHash={}", 
                proofFileId, tokenReward, txHash);
            
            // 查找证明文件
            Optional<ProofFile> proofFileOpt = proofFileRepository.findById(proofFileId);
            if (proofFileOpt.isPresent()) {
                ProofFile proofFile = proofFileOpt.get();
                proofFile.setTokenReward(tokenReward);
                proofFile.setTokenRewardTxHash(txHash);
                proofFileRepository.save(proofFile);
                
                logger.info("✅ 代币奖励已保存到数据库");
                
                response.put("success", true);
                response.put("message", "代币奖励已保存");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "证明文件不存在");
                return ResponseEntity.status(404).body(response);
            }
            
        } catch (Exception e) {
            logger.error("❌ 保存代币奖励失败", e);
            response.put("success", false);
            response.put("message", "保存失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取用户信息（花名、代表作、是否展示代表作）
     * 供手机端证明提交界面使用
     */
    @GetMapping("/user/info/{walletAddress}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String walletAddress) {
        Map<String, Object> response = new HashMap<>();
        
        logger.info("查询用户信息: {}", walletAddress);
        
        try {
            // 标准化地址（移除0x前缀）
            String normalizedAddress = walletAddress.toLowerCase();
            if (normalizedAddress.startsWith("0x")) {
                normalizedAddress = normalizedAddress.substring(2);
            }
            
            // 查询用户
            Optional<UserAccount> userOpt = userAccountRepository.findByWalletAddress(normalizedAddress);
            
            if (userOpt.isPresent()) {
                UserAccount user = userOpt.get();
                
                Map<String, Object> data = new HashMap<>();
                data.put("walletAddress", user.getWalletAddress());
                data.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
                data.put("representativeWork", user.getRepresentativeWork() != null ? user.getRepresentativeWork() : "");
                data.put("showRepresentativeWork", user.getShowRepresentativeWork() != null ? user.getShowRepresentativeWork() : false);
                
                response.put("success", true);
                response.put("data", data);
                
                logger.info("找到用户信息: 花名={}, 代表作={}, 展示={}", 
                    user.getDisplayName(), user.getRepresentativeWork(), user.getShowRepresentativeWork());
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
                logger.info("用户不存在: {}", walletAddress);
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            logger.error("查询用户信息失败", e);
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
