# 🎯 BrokerWallet 最终数据库架构

## 📊 表结构设计

### 1️⃣ 用户账户表 (user_accounts)

```sql
CREATE TABLE `user_accounts` (
    `wallet_address` VARCHAR(42) PRIMARY KEY,
    `display_name` VARCHAR(100),                    -- 用户花名（钱包前端填写）
    `representative_work` VARCHAR(500),             -- 代表作描述（钱包前端填写）
    `show_representative_work` BOOLEAN DEFAULT FALSE,  -- 用户选择：是否展示代表作
    `admin_approved_display` BOOLEAN DEFAULT FALSE,    -- 管理员是否同意展示代表作
    `gold_medals` INT DEFAULT 0,                   -- 金牌数量
    `silver_medals` INT DEFAULT 0,                 -- 银牌数量  
    `bronze_medals` INT DEFAULT 0,                 -- 铜牌数量
    `blockchain_gold_medals` INT DEFAULT 0,        -- 区块链金牌数（一致性检查）
    `blockchain_silver_medals` INT DEFAULT 0,      -- 区块链银牌数（一致性检查）
    `blockchain_bronze_medals` INT DEFAULT 0,      -- 区块链铜牌数（一致性检查）
    `blockchain_sync_time` DATETIME,               -- 最后同步时间
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `status` ENUM('ACTIVE', 'INACTIVE', 'BANNED') DEFAULT 'ACTIVE'
);
```

### 2️⃣ 证明文件表 (proof_files)

```sql
CREATE TABLE `proof_files` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `file_name` VARCHAR(255) NOT NULL,             -- 系统生成的文件名
    `original_name` VARCHAR(255) NOT NULL,         -- 用户上传的原始文件名
    `file_type` VARCHAR(100) NOT NULL,             -- 文件类型
    `file_size` BIGINT NOT NULL,                   -- 文件大小
    `file_path` VARCHAR(500) NOT NULL,             -- 存储路径
    `file_hash` VARCHAR(32),                       -- MD5哈希（防重复）
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `wallet_address` VARCHAR(42) NOT NULL,         -- 证明文件属于哪个用户
    `audit_status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    `audit_time` DATETIME,                         -- 审核时间
    `medal_awarded` ENUM('NONE', 'GOLD', 'SILVER', 'BRONZE') DEFAULT 'NONE',
    `medal_award_time` DATETIME,                   -- 勋章发放时间
    `medal_transaction_hash` VARCHAR(66),          -- 区块链交易哈希
    `status` ENUM('ACTIVE', 'DELETED') DEFAULT 'ACTIVE',
    
    FOREIGN KEY (`wallet_address`) REFERENCES `user_accounts`(`wallet_address`)
);
```

### 3️⃣ NFT图片表 (nft_images)

```sql
CREATE TABLE `nft_images` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `image_name` VARCHAR(255) NOT NULL,            -- 系统生成的图片名
    `original_name` VARCHAR(255) NOT NULL,         -- 用户上传的原始图片名
    `image_type` VARCHAR(50) NOT NULL,             -- 图片类型
    `image_size` BIGINT NOT NULL,                  -- 图片大小
    `image_path` VARCHAR(500) NOT NULL,            -- 存储路径
    `thumbnail_path` VARCHAR(500),                 -- 缩略图路径
    `base64_data` LONGTEXT,                        -- Base64数据（用于铸造）
    `image_hash` VARCHAR(32),                      -- 图片哈希（防重复）
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `wallet_address` VARCHAR(42) NOT NULL,         -- NFT属于哪个用户
    `proof_file_id` BIGINT NOT NULL,               -- 关联的证明文件
    `mint_approval_status` ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    `mint_approval_time` DATETIME,                 -- 审批时间
    `mint_status` ENUM('NOT_STARTED', 'PROCESSING', 'SUCCESS', 'FAILED') DEFAULT 'NOT_STARTED',
    `mint_time` DATETIME,                          -- NFT铸造时间
    `transaction_hash` VARCHAR(66),                -- 区块链交易哈希
    `token_id` VARCHAR(100),                       -- NFT Token ID
    `status` ENUM('ACTIVE', 'DELETED') DEFAULT 'ACTIVE',
    
    FOREIGN KEY (`wallet_address`) REFERENCES `user_accounts`(`wallet_address`),
    FOREIGN KEY (`proof_file_id`) REFERENCES `proof_files`(`id`)
);
```

## 🎯 代表作展示的双重控制机制

### 🔒 展示条件
代表作只有在**同时满足两个条件**时才会在排行榜显示：

1. **用户选择展示** (`show_representative_work = TRUE`)
2. **管理员同意展示** (`admin_approved_display = TRUE`)

### 📱 用户端操作
```
钱包前端用户填写：
┌─────────────────────────────────────┐
│ 用户花名: [区块链开发者           ] │
│                                     │
│ 代表作描述:                         │
│ ┌─────────────────────────────────┐ │
│ │ 开发了多个DeFi协议，专注于智能  │ │
│ │ 合约安全，有5年区块链开发经验   │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ☑️ 在排行榜上显示代表作              │  ← 用户选择
│                                     │
│ [保存信息]                          │
└─────────────────────────────────────┘

保存到数据库：
- show_representative_work = TRUE  （用户选择）
- admin_approved_display = FALSE   （默认值，等待管理员审核）
```

### 🖥️ 管理员端操作
```
管理员审核界面：
┌─────────────────────────────────────────────────────────┐
│ 待审核代表作展示                                        │
│                                                         │
│ 用户: 0x1234...7890 (区块链开发者)                     │
│ 代表作: 开发了多个DeFi协议，专注于智能合约安全...        │
│ 用户选择: ✅ 希望展示                                   │
│ 管理员审核: ⏳ 待审核                                   │
│                                                         │
│ 审核决定:                                              │
│ ○ 同意展示 ✅  ○ 拒绝展示 ❌                            │
│                                                         │
│ [确认审核]                                             │
└─────────────────────────────────────────────────────────┘
```

### 🏆 排行榜显示逻辑
```sql
-- 排行榜查询（双重条件检查）
SELECT 
    COALESCE(ua.display_name, '匿名用户') as display_name,
    ua.gold_medals, ua.silver_medals, ua.bronze_medals,
    CASE 
        WHEN ua.show_representative_work = TRUE 
         AND ua.admin_approved_display = TRUE 
         AND ua.representative_work IS NOT NULL 
        THEN ua.representative_work 
        ELSE '不展示代表作' 
    END as representative_work
FROM user_accounts ua
WHERE ua.status = 'ACTIVE'
ORDER BY ua.gold_medals*3 + ua.silver_medals*2 + ua.bronze_medals DESC;
```

## 📋 展示状态说明

| 用户选择 | 管理员审核 | 排行榜显示 | 说明 |
|---------|-----------|-----------|------|
| ❌ FALSE | ❌ FALSE | 不展示代表作 | 用户不想展示 |
| ❌ FALSE | ✅ TRUE  | 不展示代表作 | 用户不想展示 |
| ✅ TRUE  | ❌ FALSE | 不展示代表作 | 等待管理员审核 |
| ✅ TRUE  | ✅ TRUE  | **显示代表作** | **双重条件满足** |

## 🔄 完整业务流程

### 1️⃣ 用户填写代表作
```
用户操作：
- 在钱包前端填写 display_name 和 representative_work
- 选择 show_representative_work = TRUE
- 系统设置 admin_approved_display = FALSE（默认）
```

### 2️⃣ 管理员审核代表作
```
管理员操作：
- 查看用户填写的代表作内容
- 检查内容是否合规（无违法、无广告等）
- 决定是否同意展示：
  - 同意：admin_approved_display = TRUE
  - 拒绝：admin_approved_display = FALSE
```

### 3️⃣ 排行榜展示
```
系统逻辑：
- 检查 show_representative_work = TRUE（用户想展示）
- 检查 admin_approved_display = TRUE（管理员同意）
- 两个条件都满足时才显示代表作
- 否则显示"不展示代表作"
```

## 🎯 设计优势

1. **🔒 内容安全**：管理员可以审核代表作内容，防止不当信息
2. **👤 用户控制**：用户可以随时选择是否展示代表作
3. **⚖️ 双重保障**：需要用户同意+管理员审核才能展示
4. **🔄 灵活管理**：管理员可以随时撤销展示权限
5. **📊 清晰状态**：明确的状态字段，便于管理和查询

## 🛠️ API接口设计

### 用户更新代表作
```java
PUT /api/user/representative-work
{
    "walletAddress": "0x1234...7890",
    "displayName": "区块链开发者",
    "representativeWork": "开发了多个DeFi协议...",
    "showRepresentativeWork": true
}

响应:
{
    "success": true,
    "message": "代表作已提交，等待管理员审核"
}
```

### 管理员审核代表作
```java
POST /api/admin/approve-representative-work
{
    "walletAddress": "0x1234...7890",
    "approved": true
}

响应:
{
    "success": true,
    "message": "代表作展示状态已更新"
}
```

### 获取排行榜
```java
GET /api/ranking

响应:
{
    "success": true,
    "data": [
        {
            "rank": 1,
            "displayName": "区块链开发者",
            "goldMedals": 2,
            "silverMedals": 1,
            "bronzeMedals": 0,
            "representativeWork": "开发了多个DeFi协议，专注于智能合约安全"  // 双重条件满足时显示
        }
    ]
}
```

这个设计确保了代表作内容的安全性和可控性，同时保持了用户的自主选择权！
