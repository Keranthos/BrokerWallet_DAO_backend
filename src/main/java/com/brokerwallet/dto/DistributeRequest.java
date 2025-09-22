package com.brokerwallet.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;

/**
 * 勋章发放请求DTO
 */
@Data
public class DistributeRequest {
    
    @NotBlank(message = "目标地址不能为空")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "无效的以太坊地址")
    private String to;
    
    @Min(value = 0, message = "金牌数量不能为负数")
    private int goldQty = 0;
    
    @Min(value = 0, message = "银牌数量不能为负数")
    private int silverQty = 0;
    
    @Min(value = 0, message = "铜牌数量不能为负数")
    private int bronzeQty = 0;
}
