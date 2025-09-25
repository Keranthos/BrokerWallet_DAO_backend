package com.brokerwallet.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * NFT铸造请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftMintRequest {
    
    /**
     * NFT接收者地址
     */
    @NotBlank(message = "接收者地址不能为空")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "无效的以太坊地址格式")
    private String ownerAddress;
    
    /**
     * NFT名称
     */
    @NotBlank(message = "NFT名称不能为空")
    private String name;
    
    /**
     * NFT描述
     */
    @NotBlank(message = "NFT描述不能为空")
    private String description;
    
    /**
     * 图片数据（Base64编码）
     */
    private String imageData;
    
    /**
     * 图片类型
     */
    private String imageType;
    
    /**
     * 属性数据（JSON格式）
     */
    private String attributes;
    
    /**
     * 作者信息（用于生成默认卡片）
     */
    private String authorInfo;
    
    /**
     * 事件类型
     */
    private String eventType;
    
    /**
     * 事件描述
     */
    private String eventDescription;
    
    /**
     * 贡献等级
     */
    private String contributionLevel;
    
    /**
     * 时间戳
     */
    private String timestamp;
}
