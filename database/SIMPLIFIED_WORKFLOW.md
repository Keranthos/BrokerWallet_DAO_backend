# 📱 BrokerWallet 简化业务流程

## 🎯 设计理念：极简用户体验

**用户只需要上传文件，其他的都由系统和管理员处理。**

## 🔄 完整业务流程

### 📱 手机端用户操作

#### 1️⃣ 上传证明文件
```
用户操作：
1. 选择证明文件（PDF、图片等）
2. 选择是否作为代表作展示 ☑️
3. 点击上传

系统自动处理：
- 生成唯一文件名
- 计算文件哈希（防重复）
- 存储到文件系统
- 记录到数据库
- 状态设为 PENDING（等待审核）
```

#### 2️⃣ 上传NFT图片
```
用户操作：
1. 选择已审核通过的证明文件
2. 选择NFT图片
3. 点击上传

系统自动处理：
- 生成唯一图片名
- 生成缩略图
- 转换为Base64（用于铸造）
- 状态设为 PENDING（等待管理员批准铸造）
```

### 🖥️ 管理员网页端操作

#### 3️⃣ 审核证明文件
```
管理员操作：
1. 查看待审核的证明文件列表
2. 下载/查看文件内容
3. 根据质量决定发放勋章等级：
   - 🥇 金牌：高质量证明
   - 🥈 银牌：中等质量证明  
   - 🥉 铜牌：基础质量证明
   - ❌ 拒绝：不合格证明

系统自动处理：
- 更新audit_status
- 发放相应勋章到用户账户
- 同时更新数据库和区块链
- 检查数据一致性
```

#### 4️⃣ 批准NFT铸造
```
管理员操作：
1. 查看待批准的NFT图片列表
2. 查看关联的证明文件（必须已审核通过）
3. 决定是否同意该地址铸造NFT：
   - ✅ 同意：允许铸造
   - ❌ 拒绝：不允许铸造

用户后续操作：
- 如果获得批准，用户可以在手机端点击"铸造NFT"
- 系统自动处理区块链交易
```

## 📊 数据库表结构（简化版）

### 1️⃣ 用户账户表 (user_accounts)
```sql
-- 核心字段
wallet_address VARCHAR(42) PRIMARY KEY    -- 钱包地址作为主键
display_name VARCHAR(100)                 -- 用户花名（可选）
representative_work VARCHAR(500)          -- 代表作描述（可选）
show_representative_work BOOLEAN          -- 是否展示代表作

-- 勋章统计
gold_medals INT DEFAULT 0                 -- 金牌数量
silver_medals INT DEFAULT 0               -- 银牌数量
bronze_medals INT DEFAULT 0               -- 铜牌数量

-- 区块链同步
blockchain_gold_medals INT DEFAULT 0      -- 区块链上的金牌数
blockchain_silver_medals INT DEFAULT 0    -- 区块链上的银牌数
blockchain_bronze_medals INT DEFAULT 0    -- 区块链上的铜牌数
```

### 2️⃣ 证明文件表 (proof_files) - 简化版
```sql
-- 文件基本信息（系统自动生成）
file_name VARCHAR(255) NOT NULL           -- 系统生成的唯一文件名
original_name VARCHAR(255) NOT NULL       -- 用户上传的原始文件名
file_type VARCHAR(100) NOT NULL           -- 文件类型
file_size BIGINT NOT NULL                 -- 文件大小
file_path VARCHAR(500) NOT NULL           -- 存储路径
file_hash VARCHAR(32)                     -- 防重复哈希

-- 用户选择
wallet_address VARCHAR(42) NOT NULL       -- 上传用户
show_as_representative BOOLEAN DEFAULT FALSE  -- 是否作为代表作展示

-- 管理员审核
audit_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING'
medal_awarded ENUM('NONE', 'GOLD', 'SILVER', 'BRONZE') DEFAULT 'NONE'
medal_award_time DATETIME                 -- 勋章发放时间
medal_transaction_hash VARCHAR(66)        -- 区块链交易哈希

-- 移除的复杂字段
❌ proof_title          -- 不需要用户填写标题
❌ proof_description    -- 不需要用户填写描述  
❌ proof_category       -- 不需要分类
❌ audit_by            -- 简化审核信息
❌ audit_remarks       -- 简化审核信息
```

### 3️⃣ NFT图片表 (nft_images) - 简化版
```sql
-- 图片基本信息（系统自动生成）
image_name VARCHAR(255) NOT NULL          -- 系统生成的唯一图片名
original_name VARCHAR(255) NOT NULL       -- 用户上传的原始图片名
image_type VARCHAR(50) NOT NULL           -- 图片类型
image_size BIGINT NOT NULL                -- 图片大小
image_path VARCHAR(500) NOT NULL          -- 存储路径
thumbnail_path VARCHAR(500)               -- 缩略图路径
base64_data LONGTEXT                      -- Base64数据（用于铸造）

-- 关联信息
wallet_address VARCHAR(42) NOT NULL       -- 上传用户
proof_file_id BIGINT NOT NULL             -- 关联的证明文件（必须已审核通过）

-- 管理员批准铸造
mint_approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING'
mint_approval_time DATETIME               -- 批准时间

-- NFT铸造状态
mint_status ENUM('NOT_STARTED', 'PROCESSING', 'SUCCESS', 'FAILED') DEFAULT 'NOT_STARTED'
transaction_hash VARCHAR(66)              -- 铸造交易哈希
token_id VARCHAR(100)                     -- NFT Token ID

-- 移除的复杂字段
❌ nft_name            -- 不需要用户命名NFT
❌ nft_description     -- 不需要用户描述NFT
❌ nft_shares          -- 简化铸造参数
❌ gas_price           -- 简化铸造参数
❌ contract_address    -- 系统配置处理
```

## 🎯 用户体验对比

### ❌ 原来的复杂流程
```
用户上传证明文件时需要填写：
- 证明标题/名称 ✍️
- 证明描述 ✍️  
- 证明类别 ✍️
- 是否展示为代表作 ✍️

用户上传NFT图片时需要填写：
- NFT名称 ✍️
- NFT描述 ✍️
- NFT份数 ✍️
- Gas价格 ✍️
```

### ✅ 现在的简化流程
```
用户上传证明文件时只需要：
- 选择文件 📁
- 是否作为代表作展示 ☑️（可选）

用户上传NFT图片时只需要：
- 选择已审核的证明文件 📄
- 选择图片 🖼️
```

## 🏆 管理员审核界面设计

### 证明文件审核页面
```
待审核列表：
┌─────────────────────────────────────────────────────────┐
│ 用户地址: 0x1234...7890                                 │
│ 文件名: 学位证书.pdf                                    │
│ 上传时间: 2024-01-15 10:30:00                          │
│ 文件大小: 1.2MB                                        │
│ 是否作为代表作: ✅ 是                                   │
│                                                        │
│ [查看文件] [下载文件]                                   │
│                                                        │
│ 审核决定:                                              │
│ ○ 发放金牌 🥇  ○ 发放银牌 🥈  ○ 发放铜牌 🥉  ○ 拒绝 ❌    │
│                                                        │
│ [确认审核]                                             │
└─────────────────────────────────────────────────────────┘
```

### NFT铸造批准页面
```
待批准列表：
┌─────────────────────────────────────────────────────────┐
│ 用户地址: 0x1234...7890                                 │
│ NFT图片: my_nft_image.jpg                              │
│ 关联证明: 学位证书.pdf (已获得金牌 🥇)                   │
│ 上传时间: 2024-01-15 11:00:00                          │
│                                                        │
│ [查看图片] [查看证明文件]                                │
│                                                        │
│ 批准决定:                                              │
│ ○ 同意铸造 ✅  ○ 拒绝铸造 ❌                            │
│                                                        │
│ [确认批准]                                             │
└─────────────────────────────────────────────────────────┘
```

## 🚀 技术实现要点

### 文件上传自动处理
```java
// 用户上传文件时，系统自动处理所有信息
public ProofFile uploadProofFile(MultipartFile file, String walletAddress, boolean showAsRepresentative) {
    // 1. 生成唯一文件名
    String fileName = generateUniqueFileName(file.getOriginalFilename());
    
    // 2. 计算文件哈希
    String fileHash = calculateFileHash(file);
    
    // 3. 保存文件
    String filePath = saveFile(file, getProofDirectory());
    
    // 4. 创建数据库记录（只包含必要信息）
    ProofFile proofFile = new ProofFile();
    proofFile.setFileName(fileName);
    proofFile.setOriginalName(file.getOriginalFilename());
    proofFile.setFileType(file.getContentType());
    proofFile.setFileSize(file.getSize());
    proofFile.setFilePath(filePath);
    proofFile.setFileHash(fileHash);
    proofFile.setWalletAddress(walletAddress);
    proofFile.setShowAsRepresentative(showAsRepresentative);
    // audit_status 默认为 PENDING
    
    return proofFileRepository.save(proofFile);
}
```

### 管理员审核处理
```java
// 管理员审核时，一键发放勋章
public void approveProofFile(Long fileId, MedalType medalType) {
    ProofFile proofFile = proofFileRepository.findById(fileId);
    
    // 1. 更新文件审核状态
    proofFile.setAuditStatus(AuditStatus.APPROVED);
    proofFile.setMedalAwarded(medalType);
    proofFile.setMedalAwardTime(LocalDateTime.now());
    
    // 2. 更新用户勋章数量
    UserAccount user = userAccountRepository.findByWalletAddress(proofFile.getWalletAddress());
    switch (medalType) {
        case GOLD -> user.setGoldMedals(user.getGoldMedals() + 1);
        case SILVER -> user.setSilverMedals(user.getSilverMedals() + 1);
        case BRONZE -> user.setBronzeMedals(user.getBronzeMedals() + 1);
    }
    
    // 3. 发起区块链交易
    String txHash = blockchainService.awardMedal(user.getWalletAddress(), medalType);
    proofFile.setMedalTransactionHash(txHash);
    
    // 4. 保存更改
    proofFileRepository.save(proofFile);
    userAccountRepository.save(user);
}
```

## 🎉 简化后的优势

1. **🚀 用户体验极佳**：用户只需要上传文件，无需填写复杂信息
2. **⚡ 操作效率高**：减少用户操作步骤，降低使用门槛
3. **🔒 减少错误**：系统自动处理，减少用户输入错误
4. **🎯 专注核心**：专注于文件审核和勋章发放的核心业务
5. **📱 移动友好**：简化的界面更适合手机操作
6. **🛠️ 维护简单**：减少字段和逻辑，降低系统复杂度

这个简化设计完美平衡了功能完整性和用户体验，让DAPP真正做到了"简单易用"！
