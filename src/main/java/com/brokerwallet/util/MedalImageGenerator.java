package com.brokerwallet.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Component
@Slf4j
public class MedalImageGenerator {

    // 优化：减小图片尺寸以降低存储成本
    private static final int WIDTH = 300;  // 从400减少到300
    private static final int HEIGHT = 450; // 从600减少到450
    private static final Color BACKGROUND_COLOR = new Color(25, 25, 35);
    private static final Color BORDER_COLOR = new Color(255, 215, 0);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color TITLE_COLOR = new Color(255, 215, 0);
    private static final Color ATTRIBUTE_COLOR = new Color(200, 200, 200);

    public String generateMedalImage(String authorInfo, String eventType, String eventDescription, 
                                   String contributionLevel, String timestamp) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // 设置抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 绘制背景
            drawBackground(g2d);

            // 绘制边框
            drawBorder(g2d);

            // 绘制标题
            drawTitle(g2d);

            // 绘制内容
            drawContent(g2d, authorInfo, eventType, eventDescription, contributionLevel, timestamp);

            g2d.dispose();

            // 转换为Base64并压缩
            return convertToBase64Optimized(image);

        } catch (Exception e) {
            log.error("生成勋章图片失败", e);
            return null;
        }
    }

    private void drawBackground(Graphics2D g2d) {
        // 渐变背景
        GradientPaint gradient = new GradientPaint(0, 0, BACKGROUND_COLOR, WIDTH, HEIGHT, 
                new Color(45, 45, 65));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawBorder(Graphics2D g2d) {
        g2d.setColor(BORDER_COLOR);
        g2d.setStroke(new BasicStroke(2)); // 减少边框宽度
        g2d.drawRoundRect(8, 8, WIDTH - 16, HEIGHT - 16, 15, 15);
        
        // 内边框
        g2d.setColor(new Color(255, 215, 0, 80));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(12, 12, WIDTH - 24, HEIGHT - 24, 12, 12);
    }

    private void drawTitle(Graphics2D g2d) {
        g2d.setColor(TITLE_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 20)); // 减小字体
        
        String title = "BlockEmulator";
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = (WIDTH - fm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, 45);
        
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 16));
        String subtitle = "科研贡献勋章";
        fm = g2d.getFontMetrics();
        int subtitleX = (WIDTH - fm.stringWidth(subtitle)) / 2;
        g2d.drawString(subtitle, subtitleX, 65);
    }

    private void drawContent(Graphics2D g2d, String authorInfo, String eventType, 
                           String eventDescription, String contributionLevel, String timestamp) {
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12)); // 减小字体
        
        int y = 90;
        int lineHeight = 20; // 减少行高
        int margin = 25;
        int maxWidth = WIDTH - 2 * margin;

        // 绘制作者信息
        drawAttribute(g2d, "作者", authorInfo, y, margin, maxWidth);
        y += lineHeight * 2;

        // 绘制事件类型
        drawAttribute(g2d, "事件类型", eventType, y, margin, maxWidth);
        y += lineHeight * 2;

        // 绘制事件描述
        drawMultilineText(g2d, "事件描述", eventDescription, y, margin, maxWidth, lineHeight);
        y += getTextHeight(eventDescription, maxWidth, lineHeight) + lineHeight;

        // 绘制贡献等级
        drawAttribute(g2d, "贡献等级", contributionLevel, y, margin, maxWidth);
        y += lineHeight * 2;

        // 绘制时间戳
        String formattedTime = formatTimestamp(timestamp);
        drawAttribute(g2d, "时间", formattedTime, y, margin, maxWidth);
    }

    private void drawAttribute(Graphics2D g2d, String label, String value, int y, int margin, int maxWidth) {
        // 绘制标签
        g2d.setColor(ATTRIBUTE_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 11));
        g2d.drawString(label + ":", margin, y);
        
        // 绘制值
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        g2d.drawString(value, margin, y + 15);
    }

    private void drawMultilineText(Graphics2D g2d, String label, String text, int y, int margin, 
                                  int maxWidth, int lineHeight) {
        // 绘制标签
        g2d.setColor(ATTRIBUTE_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 11));
        g2d.drawString(label + ":", margin, y);
        
        // 绘制多行文本
        g2d.setColor(TEXT_COLOR);
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        String[] lines = wrapText(text, maxWidth, g2d.getFontMetrics());
        for (int i = 0; i < lines.length; i++) {
            g2d.drawString(lines[i], margin, y + 15 + i * lineHeight);
        }
    }

    private String[] wrapText(String text, int maxWidth, FontMetrics fm) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }

        StringBuilder sb = new StringBuilder();
        String[] words = text.split(" ");
        String currentLine = "";

        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine = testLine;
            } else {
                if (!currentLine.isEmpty()) {
                    sb.append(currentLine).append("\n");
                }
                currentLine = word;
            }
        }
        sb.append(currentLine);

        return sb.toString().split("\n");
    }

    private int getTextHeight(String text, int maxWidth, int lineHeight) {
        if (text == null || text.isEmpty()) {
            return lineHeight;
        }
        
        // 简单估算行数
        int estimatedLines = Math.max(1, text.length() / 25);
        return estimatedLines * lineHeight;
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "未知时间";
        }
        
        try {
            // 简单格式化，实际项目中可以使用更复杂的日期格式化
            return timestamp.substring(0, Math.min(timestamp.length(), 19)).replace('T', ' ');
        } catch (Exception e) {
            return timestamp;
        }
    }

    // 优化：使用更高效的压缩算法
    private String convertToBase64Optimized(BufferedImage image) throws IOException {
        // 首先尝试生成较小的JPEG图片
        BufferedImage compressedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = compressedImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // 使用JPEG格式，质量设置为0.8以平衡质量和大小
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.8f);
        
        javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(compressedImage, null, null), param);
        writer.dispose();
        ios.close();
        
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        
        log.info("生成的勋章图片大小: {} bytes", imageBytes.length);
        
        // 如果仍然太大，进一步压缩
        if (imageBytes.length > 30000) { // 30KB
            log.info("图片仍然较大，进行进一步压缩");
            return compressImageFurther(compressedImage);
        }
        
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private String compressImageFurther(BufferedImage originalImage) throws IOException {
        // 创建更小的图片
        int newWidth = WIDTH / 2;
        int newHeight = HEIGHT / 2;
        BufferedImage compressedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = compressedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        // 使用更低的质量设置
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.6f);
        
        javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(compressedImage, null, null), param);
        writer.dispose();
        ios.close();
        
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        
        log.info("进一步压缩后的图片大小: {} bytes", imageBytes.length);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // 保留旧方法以兼容
    private String convertToBase64(BufferedImage image) throws IOException {
        return convertToBase64Optimized(image);
    }

    private String compressImage(BufferedImage originalImage) throws IOException {
        return compressImageFurther(originalImage);
    }
}

