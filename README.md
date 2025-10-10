# BrokerWallet 后端服务

## 📱 项目简介

BrokerWallet后端服务是一个基于Spring Boot的区块链应用后端，为BrokerWallet DAO系统提供以下功能：

- 🏆 **勋章管理**：用户勋章发放、查询、排行榜
- 🎨 **NFT铸造**：支持用户上传图片并铸造NFT
- 📋 **材料审核**：证明材料上传、审核、批次管理
- 🔗 **区块链交互**：与BrokerChain节点通信，调用智能合约
- 💾 **数据存储**：MySQL数据库 + 本地文件系统
- 🖼️ **图片处理**：图片压缩、缩略图生成、Base64编码

---

## 🚀 快速开始

### 环境要求

- ☕ **Java 17+** (OpenJDK或Oracle JDK)
- 🗄️ **MySQL 8.0+**
- 🔧 **Maven 3.8+**
- 🔗 **BrokerChain节点** (或连接到已有节点)

---

### 方式1：自动创建数据库表（推荐）

**适用于首次部署或快速测试**

#### 1. 创建空数据库
```sql
-- 登录MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `brokerwallet` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 退出
EXIT;
```

#### 2. 配置数据库连接
编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brokerwallet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: YOUR_PASSWORD  # 修改为您的MySQL密码
  
  jpa:
    hibernate:
      ddl-auto: update  # JPA自动创建/更新表结构
```

#### 3. 启动服务
```bash
# JPA会自动创建所有表
mvn spring-boot:run
```

**优点：**
- ✅ 零配置，自动创建表
- ✅ 代码改动时自动更新表结构
- ✅ 适合开发和测试

**注意：**
- ⚠️ 生产环境建议使用方式2（更可控）

---

### 方式2：使用SQL脚本创建（生产推荐）

**适用于生产环境或需要精确控制表结构**

#### 1. 执行初始化脚本
```bash
cd database

# 方式1：命令行执行
mysql -u root -p < init.sql

# 方式2：MySQL客户端执行
# 打开MySQL Workbench/Navicat
# 连接到localhost
# 打开并执行 init.sql
```

#### 2. 修改JPA配置（可选）
编辑 `application.yml`：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 仅验证表结构，不自动修改
      # 或 ddl-auto: none  # 完全不操作表结构
```

#### 3. 启动服务
```bash
mvn spring-boot:run
```

**优点：**
- ✅ 完全掌控表结构
- ✅ 包含所有索引和优化
- ✅ 适合生产环境

---

### 配置区块链连接

编辑 `blockchain-config.yml`：

```yaml
blockchain:
  rpc:
    url: "http://127.0.0.1:8545"  # BrokerChain RPC地址
  
  account:
    address: "0xYOUR_BACKEND_ADDRESS"     # 后端账户地址
    private-key: "YOUR_PRIVATE_KEY"      # 后端账户私钥
  
  contracts:
    medal-nft:
      address: "0xYOUR_MEDAL_CONTRACT"   # 勋章合约地址
    
    nft-minter:
      address: "0xYOUR_NFT_CONTRACT"     # NFT铸造合约地址
```

**重要提示：**
- 后端账户需要有足够的BKC余额
- 后端账户需要被授予NFT铸造权限

---

### 配置服务器URL（可选）

如果需要手机或远程访问，编辑 `application.yml`：

```yaml
brokerwallet:
  server:
    # 本地开发
    url: "http://localhost:5000"
    
    # USB调试（使用电脑内网IP）
    # url: "http://192.168.1.100:5000"
    
    # 云服务器部署
    # url: "http://your-domain.com:5000"
```

**获取电脑IP：**
```bash
# Windows
ipconfig

# Linux/Mac
ifconfig
```

---

### 启动服务

#### 方式1：使用Maven
```bash
mvn spring-boot:run
```

#### 方式2：使用批处理（Windows）
```bash
start-server.bat
```

#### 方式3：打包运行
```bash
mvn clean package
java -jar target/brokerwallet-backend-1.0.0.jar
```

---

### 验证部署

#### 1. 检查服务状态
```bash
# 测试基础连接
curl http://localhost:5000/api/test

# 检查区块链连接
curl http://localhost:5000/api/blockchain/health

# 查看账户状态
curl http://localhost:5000/api/admin/account-status
```

#### 2. 检查数据库
```sql
USE brokerwallet;
SHOW TABLES;

-- 应该看到3个表：
-- user_accounts
-- proof_files
-- nft_images
```

#### 3. 查看日志
```bash
tail -f logs/brokerwallet-backend.log
```

---

## 📁 项目结构

```
BrokerWallet-backend/
├── src/main/java/com/brokerwallet/
│   ├── controller/          # REST API控制器
│   │   ├── AdminController.java          # 管理员接口
│   │   ├── FileUploadController.java     # 文件上传
│   │   ├── BlockchainController.java     # 区块链查询
│   │   └── ...
│   ├── service/             # 业务逻辑层
│   │   ├── BlockchainService.java        # 区块链交互
│   │   ├── MedalService.java             # 勋章管理
│   │   ├── FileStorageService.java       # 文件存储
│   │   └── ImageProcessingService.java   # 图片处理
│   ├── entity/              # JPA实体类
│   │   ├── ProofFile.java                # 证明文件
│   │   ├── NftImage.java                 # NFT图片
│   │   └── UserInfo.java                 # 用户信息
│   ├── repository/          # 数据访问层
│   ├── dto/                 # 数据传输对象
│   └── util/                # 工具类
├── src/main/resources/
│   └── application.yml      # 应用配置
├── contracts/               # 智能合约
│   ├── MedalNFT.sol
│   ├── OptimizedNftMinter.sol
│   └── README.md
├── database/                # 数据库脚本
│   ├── init.sql                          # 初始化脚本
│   └── FINAL_SCHEMA.md                   # 数据库文档
├── uploads/                 # 用户上传文件
│   ├── proofs/              # 证明材料
│   ├── nft-images/          # NFT图片
│   └── thumbnails/          # 缩略图
├── logs/                    # 应用日志
├── blockchain-config.yml    # 区块链配置
└── pom.xml                  # Maven配置
```

---

## 🔧 核心API接口

### 管理员接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/admin/pending` | GET | 获取待审核材料 |
| `/api/admin/approved` | GET | 获取已审核材料 |
| `/api/admin/all` | GET | 获取所有材料 |
| `/api/admin/review` | POST | 审核材料并分配勋章 |
| `/api/admin/material/{id}` | GET | 获取材料详情 |
| `/api/admin/account-status` | GET | 获取后端账户状态 |

### 区块链接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/blockchain/medals/{address}` | GET | 查询用户勋章 |
| `/api/blockchain/nft/user/{address}` | GET | 查询用户NFT |
| `/api/blockchain/nft/all` | GET | 查询所有NFT |
| `/api/blockchain/ranking` | GET | 获取勋章排行榜 |
| `/api/blockchain/health` | GET | 检查区块链连接 |

### 文件上传接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/upload/proof` | POST | 上传证明材料 |
| `/api/upload/complete` | POST | 完成批次提交 |
| `/api/upload/submissions/{address}` | GET | 获取提交历史 |
| `/api/upload/user-info/{address}` | GET | 获取用户信息 |
| `/api/upload/file/{fileName}` | GET | 下载文件 |

---

## 💾 数据库表结构

### user_accounts（用户账户表）
- 存储用户基本信息
- 勋章统计（金、银、铜）
- 代表作展示设置
- 区块链同步状态

### proof_files（证明文件表）
- 文件基本信息
- 审核状态和勋章发放
- **批次提交支持**（`submission_batch_id`）
- **NFT唯一性约束**（`nft_image_hash`）
- 代币奖励信息

### nft_images（NFT图片表）
- 图片基本信息
- NFT铸造状态
- 区块链交易信息
- Base64数据存储

详细结构请参考 `database/FINAL_SCHEMA.md`

---

## 🔍 故障排除

### 1. 数据库连接失败

**错误：** `Communications link failure` 或 `Access denied`

**解决：**
```bash
# 检查MySQL服务
# Windows
net start MySQL80

# Linux
sudo systemctl status mysql

# 验证密码
mysql -u root -p

# 检查数据库是否存在
SHOW DATABASES;
```

### 2. 区块链连接失败

**错误：** `Connection refused` 或 `Unable to connect`

**解决：**
```bash
# 检查节点是否运行
curl http://127.0.0.1:8545

# 验证RPC地址
# 编辑 blockchain-config.yml

# 查看节点日志
```

### 3. NFT铸造权限不足

**错误：** `Not authorized to mint NFT`

**解决：**
```bash
# 使用合约部署者账户授予权限
cd ../contract
npx hardhat run scripts/grant-nft-permission.js --network brokerchain

# 或在管理后台"账户状态"页面操作
```

### 4. 文件上传失败

**错误：** `File too large` 或 `Directory not found`

**解决：**
```yaml
# 编辑 application.yml
spring:
  servlet:
    multipart:
      max-file-size: 50MB  # 增加限制

# 检查uploads目录权限
mkdir -p uploads/proofs uploads/nft-images uploads/thumbnails
```

### 5. 表结构不匹配

**错误：** `Unknown column` 或 `Table doesn't exist`

**解决：**
```bash
# 方案1：重新执行初始化脚本
mysql -u root -p < database/init.sql

# 方案2：使用JPA自动更新
# application.yml: ddl-auto: update

# 方案3：手动添加缺失字段
ALTER TABLE proof_files ADD COLUMN nft_image_hash VARCHAR(64);
ALTER TABLE proof_files ADD COLUMN submission_batch_id VARCHAR(100);
```

---

## 📚 相关文档

- **完整部署指南：** `../DEPLOYMENT_GUIDE.md`
- **项目结构说明：** `../PROJECT_STRUCTURE.md`
- **数据库详细设计：** `database/FINAL_SCHEMA.md`
- **智能合约说明：** `contracts/README.md`
- **配置参考：** `../配置说明-服务器地址.md`

---

## 🛠️ 技术栈

- **Spring Boot 2.7.x** - Web框架
- **Spring Data JPA** - ORM框架
- **MySQL 8.0** - 关系数据库
- **Web3j 4.9.x** - 区块链交互
- **Maven 3.8+** - 项目管理
- **Lombok** - 简化代码
- **SLF4J + Logback** - 日志系统

---

## 📝 开发说明

### 添加新功能

1. 在 `entity` 包创建实体类
2. 在 `repository` 包创建Repository接口
3. 在 `service` 包实现业务逻辑
4. 在 `controller` 包暴露REST API
5. 更新 `init.sql` 脚本（如果修改了表结构）

### 调试技巧

```yaml
# application.yml
logging:
  level:
    com.brokerwallet: DEBUG        # 应用日志
    org.hibernate.SQL: DEBUG       # SQL日志
    org.web3j: DEBUG              # Web3j日志
```

---

## 📄 许可证

本项目采用 MIT 许可证。

---

## 🆘 获取帮助

遇到问题？

1. 查看日志：`logs/brokerwallet-backend.log`
2. 检查配置：`application.yml` 和 `blockchain-config.yml`
3. 验证数据库：`mysql -u root -p brokerwallet`
4. 测试连接：`curl http://localhost:5000/api/test`
5. 参考文档：`../DEPLOYMENT_GUIDE.md`

---

**🎉 祝您使用愉快！**
