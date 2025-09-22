package com.brokerwallet.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 勋章发放响应DTO
 */
@Data
@Builder
public class DistributeResponse {
    private boolean success;
    private String message;
    private String transactionHash;
    private String blockNumber;
    private long timestamp;
}
