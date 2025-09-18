package com.brokerwallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 文件存储配置类
 * 从application.yml中读取文件存储相关配置
 */
@Configuration
@ConfigurationProperties(prefix = "brokerwallet.file")
public class FileStorageConfig {
    
    /**
     * 文件上传根目录
     */
    private String uploadPath = "uploads/";
    
    /**
     * 允许的文件类型列表
     */
    private List<String> allowedTypes;
    
    /**
     * 最大文件大小（字节）
     */
    private long maxSize = 52428800L; // 50MB
    
    // Getter和Setter方法
    public String getUploadPath() {
        return uploadPath;
    }
    
    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }
    
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }
    
    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
    
    public long getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }
    
    /**
     * 检查文件类型是否被允许
     */
    public boolean isAllowedType(String contentType) {
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return true; // 如果没有配置限制，则允许所有类型
        }
        return allowedTypes.contains(contentType);
    }
    
    /**
     * 检查文件大小是否超过限制
     */
    public boolean isValidSize(long fileSize) {
        return fileSize <= maxSize;
    }
    
    /**
     * 获取证明文件存储目录
     */
    public String getProofFileDirectory() {
        return uploadPath + "proofs/";
    }
    
    /**
     * 获取NFT图片存储目录
     */
    public String getNftImageDirectory() {
        return uploadPath + "nft-images/";
    }
    
    /**
     * 获取缩略图存储目录
     */
    public String getThumbnailDirectory() {
        return uploadPath + "thumbnails/";
    }
}

