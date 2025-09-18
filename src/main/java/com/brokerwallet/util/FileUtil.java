package com.brokerwallet.util;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 文件处理工具类
 * 提供文件上传、存储、哈希计算等功能
 */
public class FileUtil {
    
    private static final String DATE_FORMAT = "yyyy/MM/dd";
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT);
    
    /**
     * 生成唯一的文件名
     * 格式：UUID_时间戳.扩展名
     */
    public static String generateUniqueFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = getFileExtension(originalFileName);
        return uuid + "_" + System.currentTimeMillis() + extension;
    }
    
    /**
     * 生成唯一的文件名（带MIME类型推断）
     */
    public static String generateUniqueFileName(String originalFileName, String mimeType) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = getFileExtension(originalFileName);
        
        // 如果没有扩展名，尝试从MIME类型推断
        if (extension.isEmpty() && mimeType != null) {
            extension = getExtensionFromMimeType(mimeType);
        }
        
        return uuid + "_" + System.currentTimeMillis() + extension;
    }
    
    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }
    
    /**
     * 从MIME类型推断文件扩展名
     */
    public static String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "";
        
        switch (mimeType.toLowerCase()) {
            // 图片类型
            case "image/jpeg":
            case "image/jpg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/bmp":
                return ".bmp";
            case "image/webp":
                return ".webp";
                
            // 文档类型
            case "application/pdf":
                return ".pdf";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            case "application/vnd.ms-excel":
                return ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                return ".xlsx";
            case "text/plain":
                return ".txt";
                
            // 压缩文件
            case "application/zip":
                return ".zip";
            case "application/x-rar-compressed":
                return ".rar";
                
            // 默认
            default:
                return "";
        }
    }
    
    /**
     * 根据日期创建目录结构
     * 例如：uploads/proofs/2024/01/15/
     */
    public static String createDateBasedDirectory(String baseDirectory) {
        String dateDirectory = dateFormatter.format(new Date());
        return baseDirectory + dateDirectory + "/";
    }
    
    /**
     * 根据用户ID和日期创建目录结构（性能优化版）
     * 例如：uploads/users/123/proofs/2024/01/15/
     */
    public static String createUserBasedDirectory(String baseDirectory, Long userId) {
        String dateDirectory = dateFormatter.format(new Date());
        return baseDirectory + "users/" + userId + "/" + dateDirectory + "/";
    }
    
    /**
     * 获取用户专用的证明文件目录（简化版，不按日期分类）
     */
    public static String getUserProofDirectory(Long userId) {
        return "uploads/proofs/users/" + userId + "/";
    }
    
    /**
     * 获取用户专用的NFT图片目录（简化版，不按日期分类）
     */
    public static String getUserNftDirectory(Long userId) {
        return "uploads/nft-images/users/" + userId + "/";
    }
    
    /**
     * 获取用户专用的缩略图目录
     */
    public static String getUserThumbnailDirectory(Long userId) {
        return "uploads/thumbnails/users/" + userId + "/";
    }
    
    /**
     * 确保目录存在，不存在则创建
     */
    public static void ensureDirectoryExists(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    /**
     * 保存文件到指定目录
     */
    public static String saveFile(MultipartFile file, String directory) throws IOException {
        // 确保目录存在
        ensureDirectoryExists(directory);
        
        // 生成唯一文件名（带MIME类型推断）
        String fileName = generateUniqueFileName(file.getOriginalFilename(), file.getContentType());
        String filePath = directory + fileName;
        
        // 保存文件
        Path targetPath = Paths.get(filePath);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath;
    }
    
    /**
     * 计算文件的MD5哈希值
     */
    public static String calculateFileHash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 计算已存储文件的MD5哈希值
     */
    public static String calculateFileHash(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 删除文件
     */
    public static boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 检查文件是否存在
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 获取文件大小
     */
    public static long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
    
    /**
     * 判断是否为图片文件
     */
    public static boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    /**
     * 创建图片缩略图
     */
    public static String createThumbnail(String imagePath, String thumbnailDirectory, int width, int height) throws IOException {
        ensureDirectoryExists(thumbnailDirectory);
        
        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(new File(imagePath));
        if (originalImage == null) {
            throw new IOException("无法读取图片文件: " + imagePath);
        }
        
        // 计算缩略图尺寸（保持比例）
        Dimension thumbnailSize = calculateThumbnailSize(originalImage.getWidth(), originalImage.getHeight(), width, height);
        
        // 创建缩略图
        BufferedImage thumbnailImage = new BufferedImage(
                thumbnailSize.width, thumbnailSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = thumbnailImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, thumbnailSize.width, thumbnailSize.height, null);
        g2d.dispose();
        
        // 生成缩略图文件名
        String originalFileName = Paths.get(imagePath).getFileName().toString();
        String thumbnailFileName = "thumb_" + originalFileName;
        String thumbnailPath = thumbnailDirectory + thumbnailFileName;
        
        // 保存缩略图
        String format = getImageFormat(originalFileName);
        ImageIO.write(thumbnailImage, format, new File(thumbnailPath));
        
        return thumbnailPath;
    }
    
    /**
     * 计算缩略图尺寸（保持原始图片比例）
     */
    private static Dimension calculateThumbnailSize(int originalWidth, int originalHeight, int maxWidth, int maxHeight) {
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
    private static String getImageFormat(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "JPEG";
            case ".png":
                return "PNG";
            case ".gif":
                return "GIF";
            case ".bmp":
                return "BMP";
            default:
                return "JPEG"; // 默认使用JPEG格式
        }
    }
    
    /**
     * 获取图片尺寸
     */
    public static Dimension getImageDimensions(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        if (image == null) {
            throw new IOException("无法读取图片文件: " + imagePath);
        }
        return new Dimension(image.getWidth(), image.getHeight());
    }
    
    /**
     * 格式化文件大小
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}

