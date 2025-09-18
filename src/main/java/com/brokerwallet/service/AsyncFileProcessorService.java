package com.brokerwallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 异步文件处理服务
 * 处理文件上传后的异步任务，如缩略图生成、文件哈希计算等
 */
@Service
public class AsyncFileProcessorService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncFileProcessorService.class);
    
    /**
     * 异步生成图片缩略图
     * @param originalImagePath 原始图片路径
     * @param thumbnailPath 缩略图保存路径
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     */
    @Async("fileProcessorExecutor")
    public void generateThumbnailAsync(String originalImagePath, String thumbnailPath, int maxWidth, int maxHeight) {
        try {
            logger.info("Starting async thumbnail generation: {} -> {}", originalImagePath, thumbnailPath);
            
            File originalFile = new File(originalImagePath);
            if (!originalFile.exists()) {
                logger.warn("Original image file not found: {}", originalImagePath);
                return;
            }
            
            BufferedImage originalImage = ImageIO.read(originalFile);
            if (originalImage == null) {
                logger.warn("Cannot read image file: {}", originalImagePath);
                return;
            }
            
            // 计算缩略图尺寸
            Dimension thumbnailSize = calculateThumbnailSize(
                originalImage.getWidth(), 
                originalImage.getHeight(), 
                maxWidth, 
                maxHeight
            );
            
            // 生成缩略图
            BufferedImage thumbnailImage = new BufferedImage(
                thumbnailSize.width, 
                thumbnailSize.height, 
                BufferedImage.TYPE_INT_RGB
            );
            
            Graphics2D g2d = thumbnailImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.drawImage(originalImage, 0, 0, thumbnailSize.width, thumbnailSize.height, null);
            g2d.dispose();
            
            // 确保目录存在
            File thumbnailFile = new File(thumbnailPath);
            thumbnailFile.getParentFile().mkdirs();
            
            // 保存缩略图
            String format = getImageFormat(originalImagePath);
            ImageIO.write(thumbnailImage, format, thumbnailFile);
            
            logger.info("缩略图生成成功: {} ({}x{})", thumbnailPath, thumbnailSize.width, thumbnailSize.height);
            
        } catch (IOException e) {
            logger.error("生成缩略图失败: " + originalImagePath, e);
        }
    }
    
    /**
     * 异步计算文件哈希值
     * @param filePath 文件路径
     * @return 文件SHA-256哈希值
     */
    @Async("fileProcessorExecutor")
    public void calculateFileHashAsync(String filePath) {
        try {
            logger.info("开始异步计算文件哈希: {}", filePath);
            
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                logger.warn("文件不存在: {}", filePath);
                return;
            }
            
            byte[] fileBytes = Files.readAllBytes(path);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String hash = hexString.toString();
            logger.info("文件哈希计算完成: {} -> {}", filePath, hash);
            
            // 这里可以将哈希值更新到数据库
            // TODO: 实现数据库更新逻辑
            
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("计算文件哈希失败: " + filePath, e);
        }
    }
    
    /**
     * 异步清理临时文件
     * @param tempFilePath 临时文件路径
     * @param delayMinutes 延迟删除的分钟数
     */
    @Async("fileProcessorExecutor")
    public void cleanupTempFileAsync(String tempFilePath, int delayMinutes) {
        try {
            logger.info("计划清理临时文件: {} ({}分钟后)", tempFilePath, delayMinutes);
            
            // 延迟清理
            Thread.sleep(delayMinutes * 60 * 1000L);
            
            File tempFile = new File(tempFilePath);
            if (tempFile.exists() && tempFile.delete()) {
                logger.info("临时文件清理成功: {}", tempFilePath);
            } else {
                logger.warn("临时文件清理失败或文件不存在: {}", tempFilePath);
            }
            
        } catch (InterruptedException e) {
            logger.warn("临时文件清理被中断: {}", tempFilePath);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 计算缩略图尺寸
     */
    private Dimension calculateThumbnailSize(int originalWidth, int originalHeight, int maxWidth, int maxHeight) {
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int thumbnailWidth = (int) (originalWidth * ratio);
        int thumbnailHeight = (int) (originalHeight * ratio);
        
        return new Dimension(thumbnailWidth, thumbnailHeight);
    }
    
    /**
     * 获取图片格式
     */
    private String getImageFormat(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "JPEG";
            case "png":
                return "PNG";
            case "gif":
                return "GIF";
            case "bmp":
                return "BMP";
            default:
                return "JPEG"; // 默认使用JPEG格式
        }
    }
}
