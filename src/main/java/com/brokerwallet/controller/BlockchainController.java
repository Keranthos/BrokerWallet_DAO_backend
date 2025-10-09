package com.brokerwallet.controller;

import com.brokerwallet.dto.MedalQueryResult;
import com.brokerwallet.dto.DistributeRequest;
import com.brokerwallet.dto.DistributeResponse;
import com.brokerwallet.dto.UnsignedTransactionData;
import com.brokerwallet.dto.NftMintRequest;
import com.brokerwallet.dto.NftMintResponse;
import com.brokerwallet.dto.NftQueryResult;
import com.brokerwallet.service.BlockchainService;
import com.brokerwallet.service.BlockchainSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

/**
 * 区块链操作控制器
 * 提供勋章查询、发放等区块链相关API
 */
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
@Slf4j
// @CrossOrigin 已在 WebConfig 中统一配置，此处删除避免冲突
@Validated
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final BlockchainSyncService blockchainSyncService;

    /**
     * 检查NFT铸造权限（详细版）
     */
    @GetMapping("/check-nft-permission")
    public ResponseEntity<?> checkNftPermissionDetailed() {
        try {
            log.info("Checking NFT minting permission (detailed)");
            String backendAccount = blockchainService.getAccountAddress();
            boolean hasPermission = blockchainService.hasMintPermission(backendAccount);
            java.math.BigInteger mintFeeWei = blockchainService.getMintFee();
            
            // 转换为ETH
            double mintFeeEth = 0.0;
            try {
                mintFeeEth = mintFeeWei.doubleValue() / 1e18;
            } catch (Exception e) {
                log.warn("Failed to convert mint fee: {}", mintFeeWei);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "contractAddress", "0x1bd997AE79DF9453b75b7b8D016a652a9c62E980",
                    "backendAccount", backendAccount,
                    "hasPermission", hasPermission,
                    "mintFee", String.format("%.6f", mintFeeEth)
                )
            ));
        } catch (Exception e) {
            log.error("Failed to check NFT permission", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Permission check failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * 检查NFT铸造权限
     */
    @GetMapping("/nft/check-permission")
    public ResponseEntity<?> checkMintPermission() {
        try {
            log.info("Checking NFT minting permission");
            boolean hasPermission = blockchainService.hasMintPermission(blockchainService.getAccountAddress());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "hasPermission", hasPermission,
                "accountAddress", blockchainService.getAccountAddress(),
                "message", hasPermission ? "Has minting permission" : "No minting permission"
            ));
        } catch (Exception e) {
            log.error("Failed to check mint permission", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Permission check failed: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询NFT铸造费用
     */
    @GetMapping("/nft/mint-fee")
    public ResponseEntity<?> getMintFee() {
        try {
            log.info("Querying NFT mint fee");
            java.math.BigInteger mintFee = blockchainService.getMintFee();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mintFee", mintFee.toString(),
                "mintFeeEth", mintFee.divide(java.math.BigInteger.valueOf(1000000000000000000L)).toString(),
                "message", "Mint fee queried successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to query mint fee", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Mint fee query failed: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询用户勋章
     */
    @GetMapping("/medals/{address}")
    public ResponseEntity<?> queryUserMedals(
            @PathVariable 
            @NotBlank(message = "Address cannot be empty")
            @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address")
            String address) {
        
        try {
            log.info("Querying medals for address: {}", address);
            
            // 按需同步用户数据到数据库
            blockchainSyncService.syncUserMedals(address);
            
            // 查询区块链上的最新数据
            MedalQueryResult result = blockchainService.queryUserMedals(address);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to query medals: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Query failed", e.getMessage()));
        }
    }

    /**
     * 创建未签名交易（供钱包程序签名）
     */
    @PostMapping("/create-unsigned-transaction")
    public ResponseEntity<?> createUnsignedTransaction(@Valid @RequestBody DistributeRequest request) {
        try {
            log.info("Creating unsigned transaction: {}", request);
            UnsignedTransactionData transactionData = blockchainService.createUnsignedTransaction(request);
            return ResponseEntity.ok(transactionData);
        } catch (Exception e) {
            log.error("Failed to create unsigned transaction: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to create transaction", e.getMessage()));
        }
    }

    /**
     * 发放勋章（使用钱包签名）
     */
    @PostMapping("/distribute-medals")
    public ResponseEntity<?> distributeMedals(@Valid @RequestBody DistributeRequest request) {
        try {
            log.info("Distributing medals with wallet signing: {}", request);
            DistributeResponse response = blockchainService.distributeMedalsWithWalletSigning(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to distribute medals: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse("Distribution failed", e.getMessage()));
        }
    }

    /**
     * 查询全局统计
     */
    @GetMapping("/global-stats")
    public ResponseEntity<String> queryGlobalStats() {
        try {
            String result = blockchainService.queryGlobalStats();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to query global stats: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to query global stats: " + e.getMessage());
        }
    }

    /**
     * 测试合约连接
     */
    @GetMapping("/test-contract")
    public ResponseEntity<String> testContract() {
        try {
            String result = blockchainService.testContractConnection();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Contract test failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Contract test failed: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new HealthResponse("OK", System.currentTimeMillis()));
    }

    /**
     * 铸造NFT
     */
    @PostMapping("/nft/mint")
    public ResponseEntity<NftMintResponse> mintNft(@Valid @RequestBody NftMintRequest request) {
        try {
            log.info("=== NFT Minting Request ===");
            log.info("Owner Address: {}", request.getOwnerAddress());
            log.info("NFT Name: {}", request.getName());
            log.info("Description: {}", request.getDescription());
            log.info("Image Data Length: {}", request.getImageData() != null ? request.getImageData().length() : 0);
            log.info("Image Data Preview: {}", request.getImageData() != null ? request.getImageData().substring(0, Math.min(100, request.getImageData().length())) : "null");
            log.info("Attributes: {}", request.getAttributes());
            
            NftMintResponse response = blockchainService.mintNft(request);
            
            log.info("=== NFT Minting Response ===");
            log.info("Success: {}", response.isSuccess());
            log.info("Message: {}", response.getMessage());
            log.info("Transaction Hash: {}", response.getTransactionHash());
            log.info("Token ID: {}", response.getTokenId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== NFT Minting Failed ===");
            log.error("Error Type: {}", e.getClass().getName());
            log.error("Error Message: {}", e.getMessage());
            log.error("Stack Trace: ", e);
            return ResponseEntity.badRequest().body(NftMintResponse.builder()
                    .success(false)
                    .message("NFT minting failed: " + e.getMessage())
                    .build());
        }
    }

    /**
     * 查询用户拥有的NFT
     */
    @GetMapping("/nft/user/{address}")
    public ResponseEntity<NftQueryResult> queryUserNfts(
            @PathVariable 
            @NotBlank(message = "Address cannot be empty")
            @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address")
            String address,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            log.info("Querying NFTs for address: {}, page: {}, size: {}", address, page, size);
            NftQueryResult result = blockchainService.queryUserNfts(address, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to query user NFTs: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(NftQueryResult.builder()
                    .address(address)
                    .nfts(new java.util.ArrayList<>())
                    .totalCount(0)
                    .build());
        }
    }

    /**
     * 查询所有NFT
     */
    @GetMapping("/nft/all")
    public ResponseEntity<NftQueryResult> queryAllNfts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            log.info("Querying all NFTs: page={}, size={}", page, size);
            NftQueryResult result = blockchainService.queryAllNfts(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to query all NFTs: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(NftQueryResult.builder()
                    .address("all")
                    .nfts(new java.util.ArrayList<>())
                    .totalCount(0)
                    .build());
        }
    }

    // 内部类用于错误响应
    public static class ErrorResponse {
        private String error;
        private String details;

        public ErrorResponse(String error, String details) {
            this.error = error;
            this.details = details;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }

    // 内部类用于健康检查响应
    public static class HealthResponse {
        private String status;
        private long timestamp;

        public HealthResponse(String status, long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}
