package com.brokerwallet.controller;

import com.brokerwallet.dto.MedalQueryResult;
import com.brokerwallet.dto.DistributeRequest;
import com.brokerwallet.dto.DistributeResponse;
import com.brokerwallet.dto.UnsignedTransactionData;
import com.brokerwallet.service.BlockchainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 区块链操作控制器
 * 提供勋章查询、发放等区块链相关API
 */
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Validated
public class BlockchainController {

    private final BlockchainService blockchainService;

    /**
     * 查询用户勋章
     */
    @GetMapping("/medals/{address}")
    public ResponseEntity<?> queryUserMedals(
            @PathVariable 
            @NotBlank(message = "地址不能为空")
            @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "无效的以太坊地址")
            String address) {
        
        try {
            log.info("Querying medals for address: {}", address);
            MedalQueryResult result = blockchainService.queryUserMedals(address);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to query medals: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse("查询失败", e.getMessage()));
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
            return ResponseEntity.badRequest().body(new ErrorResponse("创建交易失败", e.getMessage()));
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
            return ResponseEntity.badRequest().body(new ErrorResponse("发放失败", e.getMessage()));
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
            return ResponseEntity.badRequest().body("合约测试失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new HealthResponse("OK", System.currentTimeMillis()));
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
