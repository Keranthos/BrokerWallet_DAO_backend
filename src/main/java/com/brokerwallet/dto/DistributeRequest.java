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
    
    @NotBlank(message = "Target address cannot be empty")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid Ethereum address")
    private String to;
    
    @Min(value = 0, message = "Gold quantity cannot be negative")
    private int goldQty = 0;
    
    @Min(value = 0, message = "Silver quantity cannot be negative")
    private int silverQty = 0;
    
    @Min(value = 0, message = "Bronze quantity cannot be negative")
    private int bronzeQty = 0;
}
