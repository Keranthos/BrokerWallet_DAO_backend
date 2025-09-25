package com.brokerwallet.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * NFT铸造响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftMintResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 铸造的Token ID
     */
    private String tokenId;
    
    /**
     * 图片哈希
     */
    private String imageHash;
    
    /**
     * 元数据哈希
     */
    private String metadataHash;
    
    /**
     * 交易哈希
     */
    private String transactionHash;
}

