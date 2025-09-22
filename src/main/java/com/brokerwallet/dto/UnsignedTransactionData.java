package com.brokerwallet.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 未签名交易数据DTO
 */
@Data
@Builder
public class UnsignedTransactionData {
    private String to;
    private String data;
    private String value;
    private String gas;
    private String gasPrice;
    private String nonce;
    private String chainId;
}
