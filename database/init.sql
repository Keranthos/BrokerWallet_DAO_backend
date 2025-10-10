-- BrokerWallet后端数据库初始化脚本
-- 创建数据库和表结构（包含所有最新字段）
-- 适用于从零开始部署项目

-- ===================================
-- 1. 创建数据库
-- ===================================
CREATE DATABASE IF NOT EXISTS `brokerwallet` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `brokerwallet`;

-- ===================================
-- 2. 创建用户账户表
-- ===================================
CREATE TABLE IF NOT EXISTS `user_accounts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID（数字主键，性能优化）',
    `wallet_address` VARCHAR(42) NOT NULL UNIQUE COMMENT '钱包地址（唯一约束）',
    `display_name` VARCHAR(100) COMMENT '用户花名（用户在钱包前端填写，可选）',
    `representative_work` VARCHAR(500) COMMENT '代表作描述（用户在钱包前端填写，可选）',
    `show_representative_work` BOOLEAN DEFAULT FALSE COMMENT '用户选择：是否在排行榜展示代表作',
    `admin_approved_display` BOOLEAN DEFAULT FALSE COMMENT '管理员是否同意展示代表作',
    
    -- 勋章统计
    `gold_medals` INT DEFAULT 0 COMMENT '金牌数量',
    `silver_medals` INT DEFAULT 0 COMMENT '银牌数量',
    `bronze_medals` INT DEFAULT 0 COMMENT '铜牌数量',
    `total_medal_score` INT GENERATED ALWAYS AS (gold_medals * 3 + silver_medals * 2 + bronze_medals) STORED COMMENT '总勋章分数（计算列，排行榜优化）',
    
    -- 统计字段
    `total_proofs` INT DEFAULT 0 COMMENT '总证明文件数',
    `total_nfts` INT DEFAULT 0 COMMENT '总NFT数量',
    `verified_proofs` INT DEFAULT 0 COMMENT '已审核通过的证明数',
    
    -- 区块链同步
    `blockchain_sync_time` DATETIME COMMENT '最后一次与区块链同步时间',
    `blockchain_gold_medals` INT DEFAULT 0 COMMENT '区块链上的金牌数量（用于一致性检查）',
    `blockchain_silver_medals` INT DEFAULT 0 COMMENT '区块链上的银牌数量（用于一致性检查）',
    `blockchain_bronze_medals` INT DEFAULT 0 COMMENT '区块链上的铜牌数量（用于一致性检查）',
    
    -- 时间戳
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE' COMMENT '账户状态',
    
    -- 优化索引
    INDEX `idx_wallet_address` (`wallet_address`),
    INDEX `idx_medal_ranking` (`total_medal_score` DESC, `gold_medals` DESC, `silver_medals` DESC),
    INDEX `idx_admin_approved_display` (`admin_approved_display`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户账户表';

-- ===================================
-- 3. 创建证明文件表（包含新增字段）
-- ===================================
CREATE TABLE IF NOT EXISTS `proof_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_name` VARCHAR(255) NOT NULL COMMENT '系统生成的文件名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '用户上传的原始文件名',
    `file_type` VARCHAR(100) NOT NULL COMMENT '文件类型/MIME类型',
    `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    `file_hash` VARCHAR(32) COMMENT '文件MD5哈希值（防重复）',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `user_account_id` BIGINT NOT NULL COMMENT '关联用户账户ID（数字外键，性能优化）',
    
    -- 审核相关
    `audit_status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    `audit_time` DATETIME COMMENT '审核时间',
    
    -- 勋章相关
    `medal_awarded` ENUM('NONE', 'GOLD', 'SILVER', 'BRONZE') DEFAULT 'NONE' COMMENT '管理员发放的勋章类型',
    `medal_award_time` DATETIME COMMENT '勋章发放时间',
    `medal_transaction_hash` VARCHAR(66) COMMENT '勋章发放的区块链交易哈希',
    
    -- 代币奖励相关
    `token_reward` DECIMAL(30, 18) COMMENT '代币奖励数量（BKC，以Token为单位）',
    `token_reward_tx_hash` VARCHAR(66) COMMENT '代币奖励交易哈希',
    
    -- NFT唯一性约束（新增）
    `nft_image_hash` VARCHAR(64) COMMENT 'NFT图片的SHA-256哈希值，用于检查唯一性',
    
    -- 批次提交支持（新增）
    `submission_batch_id` VARCHAR(100) COMMENT '提交批次ID，标识同一次提交的多个文件，格式：BATCH_{userId}_{timestamp}',
    
    -- 状态
    `status` ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '文件状态',
    
    -- 索引优化
    INDEX `idx_user_account_id` (`user_account_id`),
    INDEX `idx_audit_status` (`audit_status`),
    INDEX `idx_medal_awarded` (`medal_awarded`),
    INDEX `idx_upload_time` (`upload_time`),
    INDEX `idx_file_hash` (`file_hash`),
    INDEX `idx_nft_image_hash` (`nft_image_hash`),
    INDEX `idx_submission_batch_id` (`submission_batch_id`),
    
    FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='证明文件表';

-- ===================================
-- 4. 创建NFT图片表
-- ===================================
CREATE TABLE IF NOT EXISTS `nft_images` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `image_name` VARCHAR(255) NOT NULL COMMENT '系统生成的图片文件名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '用户上传的原始图片名',
    `image_type` VARCHAR(50) NOT NULL COMMENT '图片类型',
    `image_size` BIGINT NOT NULL COMMENT '图片大小（字节）',
    `image_path` VARCHAR(500) NOT NULL COMMENT '图片存储路径',
    `thumbnail_path` VARCHAR(500) COMMENT '缩略图路径',
    `base64_data` LONGTEXT COMMENT 'Base64编码的图片数据（用于NFT铸造）',
    `image_hash` VARCHAR(32) COMMENT '图片哈希值（防重复）',
    `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `user_account_id` BIGINT NOT NULL COMMENT '关联用户账户ID（数字外键）',
    `proof_file_id` BIGINT NOT NULL COMMENT '关联的证明文件ID（强关联）',
    
    -- 审批相关
    `mint_approval_status` ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '管理员审批状态：是否同意该地址铸造NFT',
    `mint_approval_time` DATETIME COMMENT '审批时间',
    
    -- NFT铸造相关
    `mint_status` ENUM('NOT_STARTED', 'PROCESSING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'NFT铸造状态',
    `mint_time` DATETIME COMMENT 'NFT铸造时间',
    `transaction_hash` VARCHAR(66) COMMENT '区块链交易哈希',
    `token_id` VARCHAR(100) COMMENT 'NFT Token ID',
    
    -- 状态
    `status` ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE' COMMENT '图片状态',
    
    -- 索引优化
    INDEX `idx_user_account_id` (`user_account_id`),
    INDEX `idx_proof_file_id` (`proof_file_id`),
    INDEX `idx_mint_approval_status` (`mint_approval_status`),
    INDEX `idx_mint_status` (`mint_status`),
    INDEX `idx_upload_time` (`upload_time`),
    INDEX `idx_image_hash` (`image_hash`),
    
    FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`proof_file_id`) REFERENCES `proof_files`(`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='NFT图片表';

-- ===================================
-- 5. 显示创建的表
-- ===================================
SHOW TABLES;

-- ===================================
-- 6. 完成提示
-- ===================================
SELECT '✅ 数据库初始化完成！' as message, 
       '已创建: user_accounts, proof_files, nft_images' as tables,
       '包含所有最新字段（批次提交、NFT唯一性等）' as features;

-- ===================================
-- 使用说明
-- ===================================
-- 1. 确保MySQL已安装并运行
-- 2. 使用以下命令执行此脚本：
--    mysql -u root -p < init.sql
-- 3. 或在MySQL客户端中执行：
--    source /path/to/init.sql
-- 4. 修改 application.yml 中的数据库连接信息
-- 5. 启动后端服务，JPA会自动维护表结构
