# BrokerWallet 后端项目结构说明

## 📁 项目概览

BrokerWallet-backend 是一个基于 Spring Boot 的后端服务，负责处理用户证明材料上传、审核、勋章分配、NFT铸造以及区块链交互等核心功能。

---

## 🗂️ 目录结构详解

```
BrokerWallet-backend/
├── src/main/java/com/brokerwallet/          # Java源代码
│   ├── BrokerWalletBackendApplication.java  # 应用主入口
│   │
│   ├── config/                              # 配置类
│   │   ├── AsyncConfig.java                 # 异步任务配置
│   │   ├── BlockchainConfig.java            # 区块链连接配置
│   │   ├── FileStorageConfig.java           # 文件存储配置
│   │   └── WebConfig.java                   # Web跨域配置
│   │
│   ├── controller/                          # REST API控制器
│   │   ├── AdminController.java             # 管理员接口（材料审核、批次查询）
│   │   ├── FileUploadController.java        # 文件上传接口（证明材料、NFT图片）
│   │   ├── BlockchainController.java        # 区块链查询接口（健康检查、账户信息）
│   │   ├── MedalRankingController.java      # 勋章排行榜接口
│   │   ├── SystemController.java            # 系统信息接口
│   │   ├── TestDataController.java          # 测试数据接口
│   │   └── QuickTestController.java         # 快速测试接口
│   │
│   ├── service/                             # 业务逻辑层
│   │   ├── BlockchainService.java           # 区块链交互服务（核心）
│   │   │   ├── 勋章分配与查询
│   │   │   ├── NFT铸造（默认样式 + 用户图片）
│   │   │   ├── NFT分页查询（全局/用户）
│   │   │   ├── 代币奖励发放
│   │   │   └── 合约调用与事件监听
│   │   ├── BlockchainSyncService.java       # 区块链数据同步服务
│   │   ├── AsyncFileProcessorService.java   # 异步文件处理服务
│   │   └── UserAccountService.java          # 用户账户服务
│   │
│   ├── entity/                              # JPA实体类（数据库表映射）
│   │   ├── ProofFile.java                   # 证明文件表
│   │   │   ├── 文件基本信息（路径、类型、大小）
│   │   │   ├── 审核状态（待审核/通过/拒绝）
│   │   │   ├── 勋章类型（金/银/铜）
│   │   │   ├── NFT信息（图片哈希、铸造状态）
│   │   │   ├── 批次ID（submission_batch_id）
│   │   │   └── 代币奖励信息
│   │   ├── NftImage.java                    # NFT图片表
│   │   │   ├── 图片信息（路径、哈希、缩略图）
│   │   │   ├── 铸造状态（待铸造/已铸造/失败）
│   │   │   └── Token ID
│   │   └── UserAccount.java                 # 用户账户表
│   │       ├── 钱包地址
│   │       ├── 显示昵称
│   │       └── 代表作品
│   │
│   ├── repository/                          # 数据访问层（JPA Repository）
│   │   ├── ProofFileRepository.java         # 证明文件数据访问
│   │   │   ├── findAllDistinctBatchIds()    # 查询所有批次（按时间倒序）
│   │   │   ├── findBySubmissionBatchId()    # 按批次ID查询
│   │   │   └── 其他自定义查询
│   │   ├── NftImageRepository.java          # NFT图片数据访问
│   │   └── UserAccountRepository.java       # 用户账户数据访问
│   │
│   ├── dto/                                 # 数据传输对象
│   │   ├── NftMintRequest.java              # NFT铸造请求
│   │   ├── NftMintResponse.java             # NFT铸造响应
│   │   ├── NftQueryResult.java              # NFT查询结果
│   │   ├── MedalQueryResult.java            # 勋章查询结果
│   │   ├── DistributeRequest.java           # 勋章分配请求
│   │   ├── DistributeResponse.java          # 勋章分配响应
│   │   └── UnsignedTransactionData.java     # 未签名交易数据
│   │
│   └── util/                                # 工具类
│       ├── FileUtil.java                    # 文件操作工具
│       └── MedalImageGenerator.java         # 勋章图片生成工具
│
├── src/main/resources/                      # 资源文件
│   └── application.yml                      # 应用配置文件
│
├── contracts/                               # 智能合约（Solidity）
│   ├── MedalNFT.sol                         # 勋章系统合约
│   ├── OptimizedNftMinter.sol               # NFT铸造合约（优化版）
│   └── README.md                            # 合约说明文档
│
├── database/                                # 数据库相关
│   ├── init.sql                             # 数据库初始化脚本
│   └── FINAL_SCHEMA.md                      # 数据库结构说明文档
│
├── uploads/                                 # 用户上传文件存储
│   ├── proofs/                              # 证明材料
│   │   └── users/{userId}/                  # 按用户ID分类
│   ├── nft-images/                          # NFT原图
│   │   └── users/{userId}/
│   └── thumbnails/                          # NFT缩略图
│       └── users/{userId}/
│
├── logs/                                    # 应用日志
│   └── brokerwallet-backend.log             # 主日志文件
│
├── target/                                  # Maven构建输出
│   ├── classes/                             # 编译后的class文件
│   └── brokerwallet-backend-1.0.0.jar       # 打包后的jar文件
│
├── blockchain-config.yml                    # 区块链配置文件（重要！）
├── start-server.bat                         # Windows启动脚本
├── pom.xml                                  # Maven依赖配置
└── README.md                                # 项目说明文档
```

---

## 🔑 核心文件说明

### 1. 配置文件

#### `application.yml`
**位置：** `src/main/resources/application.yml`

**主要配置项：**
```yaml
server:
  port: 5000                    # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brokerwallet
    username: root
    password: YOUR_PASSWORD     # ⚠️ 需要修改

brokerwallet:
  server:
    url: "http://localhost:5000"  # ⚠️ 云部署时需要修改
  file:
    upload-path: uploads/
    max-size: 52428800          # 50MB
```

#### `blockchain-config.yml`
**位置：** 项目根目录

**主要配置项：**
```yaml
blockchain:
  rpc-url: "http://127.0.0.1:8545"          # ⚠️ 区块链节点地址
  account-address: "0x..."                   # ⚠️ 后端账户地址
  contracts:
    medal-contract: "0x..."                  # ⚠️ 勋章合约地址
    nft-contract: "0x..."                    # ⚠️ NFT合约地址
    token-contract: "0x..."                  # ⚠️ 代币合约地址
```

**⚠️ 重要提示：** 此文件包含私钥信息，请勿提交到公共代码仓库！

---

### 2. 核心业务类

#### `BlockchainService.java`
**位置：** `src/main/java/com/brokerwallet/service/BlockchainService.java`

**核心功能：**
- ✅ **勋章分配：** `distributeMedals()` - 调用智能合约分配勋章
- ✅ **勋章查询：** `queryUserMedals()` - 查询用户勋章数量
- ✅ **NFT铸造：** `mintNFT()` - 铸造NFT（支持默认样式和用户图片）
- ✅ **NFT查询：** `queryAllNfts()`, `queryUserNfts()` - 分页查询NFT
- ✅ **代币奖励：** `sendTokenReward()` - 发放BKC代币奖励
- ✅ **账户信息：** `getAccountBalance()`, `getAccountInfo()` - 查询账户余额和权限

**关键代码片段：**
```java
// NFT分页查询（倒序）
public NftQueryResult queryAllNfts(int page, int size) {
    int totalCount = totalSupply.intValue();
    int startTokenId = totalCount - (page * size);  // 从最大Token ID开始
    int endTokenId = Math.max(startTokenId - size + 1, 1);
    
    for (int tokenId = startTokenId; tokenId >= endTokenId; tokenId--) {
        // 查询每个NFT的详细信息
    }
}
```

#### `AdminController.java`
**位置：** `src/main/java/com/brokerwallet/controller/AdminController.java`

**核心功能：**
- ✅ **批次查询：** `getAllUsers()` - 查询所有提交批次（按时间倒序）
- ✅ **材料审核：** `updateAuditStatus()` - 更新审核状态
- ✅ **勋章分配：** `distributeMedals()` - 批量分配勋章
- ✅ **批次详情：** `getBatchDetail()` - 查询批次内所有文件

**API端点：**
```
GET  /api/admin/users?status=PENDING     # 查询待审核批次
POST /api/admin/audit                    # 更新审核状态
POST /api/admin/distribute-medals        # 分配勋章
GET  /api/admin/batch/{batchId}          # 查询批次详情
```

#### `FileUploadController.java`
**位置：** `src/main/java/com/brokerwallet/controller/FileUploadController.java`

**核心功能：**
- ✅ **批量上传：** 支持一次提交多个证明文件
- ✅ **NFT图片上传：** 支持上传自定义NFT图片
- ✅ **图片唯一性检查：** 防止重复NFT图片
- ✅ **文件类型验证：** 仅允许指定类型文件
- ✅ **自动生成缩略图：** 压缩图片并生成缩略图

**API端点：**
```
POST /api/files/upload-batch              # 批量上传证明文件
POST /api/files/upload-nft-image          # 上传NFT图片
GET  /api/files/submission-history        # 查询提交历史
```

---

### 3. 数据库实体

#### `ProofFile.java`
**关键字段：**
```java
@Entity
public class ProofFile {
    @Id
    private Long id;
    
    private String walletAddress;              // 用户钱包地址
    private String fileName;                   // 文件名
    private String filePath;                   // 文件路径
    private String fileType;                   // 文件类型
    
    @Enumerated(EnumType.STRING)
    private AuditStatus auditStatus;           // 审核状态（PENDING/APPROVED/REJECTED）
    
    @Enumerated(EnumType.STRING)
    private MedalType medalAwarded;            // 勋章类型（GOLD/SILVER/BRONZE/NONE）
    
    private String submissionBatchId;          // 批次ID（重要！）
    private String nftImageHash;               // NFT图片哈希（唯一性约束）
    private BigDecimal tokenReward;            // 代币奖励数量
    private String tokenRewardTxHash;          // 代币奖励交易哈希
    
    private LocalDateTime uploadTime;          // 上传时间
    private LocalDateTime auditTime;           // 审核时间
}
```

#### `NftImage.java`
**关键字段：**
```java
@Entity
public class NftImage {
    @Id
    private Long id;
    
    private String walletAddress;              // 所有者地址
    private String imagePath;                  // 图片路径
    private String thumbnailPath;              // 缩略图路径
    private String imageHash;                  // 图片哈希（SHA-256）
    
    @Enumerated(EnumType.STRING)
    private MintStatus mintStatus;             // 铸造状态（PENDING/MINTED/FAILED）
    
    private BigInteger tokenId;                // NFT Token ID
    private String txHash;                     // 铸造交易哈希
    
    private LocalDateTime uploadTime;          // 上传时间
    private LocalDateTime mintTime;            // 铸造时间
}
```

---

## 🔄 核心业务流程

### 1. 证明材料提交与审核流程

```
用户提交 → FileUploadController.uploadBatch()
         ↓
    保存文件到 uploads/proofs/users/{userId}/
         ↓
    生成批次ID (submission_batch_id)
         ↓
    保存记录到数据库 (ProofFile表)
         ↓
    返回提交成功 → 用户可在"提交历史"查看
         ↓
    管理员审核 → AdminController.updateAuditStatus()
         ↓
    更新审核状态 (APPROVED/REJECTED)
         ↓
    分配勋章 → AdminController.distributeMedals()
         ↓
    调用区块链 → BlockchainService.distributeMedals()
         ↓
    记录交易哈希 → 完成
```

### 2. NFT铸造流程

```
用户上传NFT图片 → FileUploadController.uploadNftImage()
         ↓
    计算图片哈希 (SHA-256)
         ↓
    检查唯一性 (nft_image_hash)
         ↓
    生成缩略图 (200x200)
         ↓
    保存到 uploads/nft-images/ 和 uploads/thumbnails/
         ↓
    保存记录到 NftImage表
         ↓
    审核通过后 → BlockchainService.mintNFT()
         ↓
    调用智能合约铸造NFT
         ↓
    更新 tokenId 和 txHash
         ↓
    更新 mintStatus = MINTED
         ↓
    用户可在"我的NFT"查看
```

### 3. NFT查询流程（分页）

```
移动端请求 → BlockchainController.queryAllNfts(page, size)
         ↓
    调用 BlockchainService.queryAllNfts()
         ↓
    查询合约 totalSupply()
         ↓
    倒序遍历 Token ID (从最新到最旧)
         ↓
    查询每个NFT的 tokenURI 和 ownerOf
         ↓
    从数据库查询图片路径和时间信息
         ↓
    组装 NftQueryResult
         ↓
    返回给移动端 → 显示在列表中
```

---

## 🛠️ 技术栈

### 核心框架
- **Spring Boot 2.7.x** - 应用框架
- **Spring Data JPA** - 数据访问
- **Hibernate** - ORM框架
- **MySQL 8.0** - 数据库

### 区块链交互
- **Web3j 4.9.x** - 以太坊Java客户端
- **BrokerChain** - 自定义区块链网络

### 文件处理
- **Apache Commons IO** - 文件操作
- **Thumbnailator** - 图片压缩

### 其他
- **Lombok** - 简化Java代码
- **Jackson** - JSON序列化
- **SLF4J + Logback** - 日志框架

---

## 📊 数据库表结构

### 主要表

#### `proof_files` - 证明文件表
```sql
CREATE TABLE proof_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(42) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    audit_status VARCHAR(20) DEFAULT 'PENDING',
    medal_awarded VARCHAR(20) DEFAULT 'NONE',
    submission_batch_id VARCHAR(100),        -- 批次ID
    nft_image_hash VARCHAR(64),              -- NFT图片哈希
    token_reward DECIMAL(10,2),              -- 代币奖励
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch (submission_batch_id),
    INDEX idx_wallet (wallet_address),
    INDEX idx_status (audit_status)
);
```

#### `nft_images` - NFT图片表
```sql
CREATE TABLE nft_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(42) NOT NULL,
    image_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    image_hash VARCHAR(64) UNIQUE,           -- 唯一性约束
    mint_status VARCHAR(20) DEFAULT 'PENDING',
    token_id BIGINT,
    tx_hash VARCHAR(66),
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    mint_time DATETIME,
    INDEX idx_wallet (wallet_address),
    INDEX idx_hash (image_hash)
);
```

#### `user_accounts` - 用户账户表
```sql
CREATE TABLE user_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_address VARCHAR(42) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    representative_work TEXT,
    show_representative_work BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 🚀 启动方式

### 开发环境启动

```bash
# 1. 确保MySQL已启动
net start MySQL80

# 2. 确保数据库已创建
mysql -u root -p < database/init.sql

# 3. 修改配置文件
# 编辑 application.yml 和 blockchain-config.yml

# 4. 启动后端
cd BrokerWallet-backend
mvn spring-boot:run

# 或使用启动脚本
start-server.bat
```

### 生产环境部署

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 运行jar包
java -jar target/brokerwallet-backend-1.0.0.jar

# 3. 后台运行（Linux）
nohup java -jar target/brokerwallet-backend-1.0.0.jar > logs/app.log 2>&1 &
```

---

## 📝 API接口概览

### 管理员接口
```
GET  /api/admin/users?status={status}     # 查询批次列表
GET  /api/admin/batch/{batchId}           # 查询批次详情
POST /api/admin/audit                     # 更新审核状态
POST /api/admin/distribute-medals         # 分配勋章
```

### 文件上传接口
```
POST /api/files/upload-batch              # 批量上传证明文件
POST /api/files/upload-nft-image          # 上传NFT图片
GET  /api/files/submission-history        # 查询提交历史
```

### 区块链查询接口
```
GET  /api/blockchain/health               # 健康检查
GET  /api/blockchain/account-info         # 账户信息
GET  /api/blockchain/nfts/all             # 查询所有NFT（分页）
GET  /api/blockchain/nfts/user/{address}  # 查询用户NFT（分页）
```

### 勋章排行接口
```
GET  /api/medals/ranking                  # 勋章排行榜
GET  /api/medals/user/{address}           # 查询用户勋章
GET  /api/medals/global-stats             # 全局统计
```

---

## 🔒 安全注意事项

1. **私钥保护**
   - ⚠️ `blockchain-config.yml` 包含私钥，切勿提交到公共仓库
   - 建议使用环境变量或密钥管理服务

2. **数据库密码**
   - ⚠️ `application.yml` 中的数据库密码需要妥善保管
   - 生产环境使用强密码

3. **文件上传安全**
   - ✅ 已实现文件类型白名单验证
   - ✅ 已实现文件大小限制（50MB）
   - ✅ 文件存储在服务器本地，不直接暴露路径

4. **跨域配置**
   - 生产环境需要限制允许的域名
   - 当前配置允许所有来源（开发用）

---

## 📚 相关文档

- **部署指南：** `../DEPLOYMENT_GUIDE.md`
- **数据库结构：** `database/FINAL_SCHEMA.md`
- **智能合约：** `contracts/README.md`
- **项目总览：** `../PROJECT_STRUCTURE.md`

---

## 🆘 常见问题

### Q1: 启动时提示数据库连接失败
**解决：** 检查 `application.yml` 中的数据库配置，确保MySQL已启动且密码正确。

### Q2: 区块链连接失败
**解决：** 检查 `blockchain-config.yml` 中的RPC地址，确保区块链节点正在运行。

### Q3: 文件上传失败
**解决：** 检查 `uploads/` 目录是否存在且有写入权限。

### Q4: NFT铸造权限不足
**解决：** 确保后端账户已被授予NFT铸造权限（使用合约部署者账户授权）。

---

**最后更新：** 2025年10月10日

