package com.brokerwallet.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * NFT查询结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NftQueryResult {
    
    /**
     * 用户地址
     */
    private String address;
    
    /**
     * NFT列表
     */
    private List<NftInfo> nfts;
    
    /**
     * 总数量
     */
    private int totalCount;
    
    /**
     * 当前页
     */
    private int currentPage;
    
    /**
     * 页大小
     */
    private int pageSize;
    
    /**
     * 总页数
     */
    private int totalPages;
    
    /**
     * NFT信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NftInfo {
        
        /**
         * Token ID
         */
        private String tokenId;
        
        /**
         * 所有者地址
         */
        private String ownerAddress;
        
        /**
         * NFT名称
         */
        private String name;
        
        /**
         * NFT描述
         */
        private String description;
        
        /**
         * 图片URL
         */
        private String imageUrl;
        
        /**
         * 属性列表
         */
        private List<Object> attributes;
    }
    
    /**
     * NFT元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NftMetadata {
        
        /**
         * NFT名称
         */
        private String name;
        
        /**
         * NFT描述
         */
        private String description;
        
        /**
         * 图片URL
         */
        private String image;
        
        /**
         * 属性列表
         */
        private List<Object> attributes;
    }
}

