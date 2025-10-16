# BrokerWallet 本地部署完整指南

本指南详细说明如何在本地电脑上从零开始部署整个BrokerWallet系统（后端 + 前端 + 移动端）。

---

## 📋 前置要求

### 必需软件

| 软件 | 版本要求 | 用途 | 下载地址 |
|------|---------|------|---------|
| Java JDK | 17+ | 运行后端 | https://adoptium.net/ |
| Node.js | 18+ | 运行前端 | https://nodejs.org/ |
| MySQL | 8.0+ | 数据库 | https://dev.mysql.com/downloads/ |
| Maven | 3.8+ | 构建后端 | https://maven.apache.org/ |
| Android Studio | 最新版 | 开发移动应用 | https://developer.android.com/studio |
| Git | 最新版 | 版本控制 | https://git-scm.com/ |

### 区块链节点

- **BrokerChain节点** - 需要运行本地节点或连接到测试网节点
- **管理员账户** - 需要有足够BKC余额的账户用于后端操作

### 硬件要求

- **内存：** 至少8GB RAM
- **存储：** 至少10GB可用空间
- **网络：** 稳定的网络连接

---

## 🚀 完整部署流程

### 步骤1：准备区块链环境

#### 1.1 启动BrokerChain节点

```bash
# 假设您已经有BrokerChain节点程序
# 启动节点（示例命令，根据实际情况调整）
cd /path/to/brokerchain
./start-node.sh

# 或使用已有的测试网节点
# 记录节点RPC地址，例如：http://127.0.0.1:56873
```

**重要信息记录：**
- ✅ RPC地址：`http://127.0.0.1:56873`（示例）
- ✅ 管理员账户地址：`0x8c056ccb92c567da3fee27c23d4f2f107f203879`
- ✅ 管理员账户私钥：`114737911582466852051274090883801946221241082675647104558725103141732245958742`

#### 1.2 部署智能合约（如果还没有）

```bash
cd contract  # 您的合约项目目录

# 安装依赖
npm install

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入管理员账户私钥和RPC地址

# 部署勋章合约
npx hardhat run scripts/deploy-medal-nft.js --network brokerchain

# 部署NFT铸造合约
npx hardhat run scripts/deploy-optimized-nft.js --network brokerchain

# 记录合约地址
# 勋章合约地址：0x1a202bfa10ea97a742ad22fcb1a7913821bf1b18
# NFT合约地址：0x1bd997AE79DF9453b75b7b8D016a652a9c62E980
```

---

### 步骤2：配置数据库

#### 2.1 启动MySQL服务

```bash
# Windows
net start MySQL80

# Linux/Mac
sudo systemctl start mysql
# 或
sudo service mysql start
```

#### 2.2 创建数据库

```bash
# 登录MySQL
mysql -u root -p
# 输入您的MySQL root密码

# 创建数据库
CREATE DATABASE brokerwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 退出
exit;
```

#### 2.3 初始化数据库（两种方式）

**⚠️ 重要：请根据您的需求选择以下两种方式之一**

---

##### 方式A：使用示例数据（推荐用于快速体验）✅

**适用场景：**
- ✅ 想要快速体验完整功能
- ✅ 查看已有的NFT和提交记录
- ✅ 学习和参考现有数据

**步骤：**

```bash
cd D:\brokerwallet\BrokerWallet-backend\database

# 1. 导入表结构
mysql -u root -p brokerwallet < init.sql

# 2. 导入示例数据
mysql -u root -p brokerwallet < sample-data.sql

# 3. 验证数据
mysql -u root -p
USE brokerwallet;
SELECT COUNT(*) FROM proof_files;   -- 应该有约15条记录
SELECT COUNT(*) FROM nft_images;    -- 应该有约8条记录
SELECT COUNT(*) FROM user_accounts; -- 应该有2条记录
EXIT;
```

**⚠️ 重要提示：**
- ✅ **保留 `uploads/` 文件夹**：不要删除或清空，因为数据库记录指向这些文件
- ✅ **文件路径已配置**：所有路径都是相对路径，可以移动整个项目文件夹
- ✅ **示例数据包含**：
  - 2个测试用户
  - 约15条证明文件提交记录
  - 约8个已铸造的NFT
  - 40个实际文件（11.76 MB）

**包含的示例用户：**
- 用户1：`0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb`（有多个NFT和勋章）
- 管理员：`0x8c056ccb92c567da3fee27c23d4f2f107f203879`

---

##### 方式B：从零开始（空数据库）✅

**适用场景：**
- ✅ 想要一个全新的系统
- ✅ 自己测试和开发
- ✅ 不需要示例数据

**步骤：**

```bash
cd D:\brokerwallet\BrokerWallet-backend\database

# 1. 仅导入表结构
mysql -u root -p brokerwallet < init.sql

# 2. 验证表已创建
mysql -u root -p
USE brokerwallet;
SHOW TABLES;
# 应该看到：proof_files, nft_images, user_accounts
SELECT COUNT(*) FROM proof_files;   -- 应该是0
EXIT;

# 3. 清空 uploads 文件夹（可选）
cd D:\brokerwallet\BrokerWallet-backend
# Windows PowerShell
Remove-Item uploads\* -Recurse -Force
# 重新创建目录结构
New-Item -ItemType Directory -Path uploads\proofs, uploads\nft-images, uploads\thumbnails -Force

# 或 Linux/Mac
# rm -rf uploads/*
# mkdir -p uploads/proofs uploads/nft-images uploads/thumbnails
```

**结果：**
- ✅ 数据库表已创建但为空
- ✅ uploads 文件夹为空
- ✅ 可以开始自己的测试

---

##### 方式对比

| 特性 | 方式A（示例数据） | 方式B（空数据库） |
|------|------------------|------------------|
| 数据库表 | ✅ 已创建 | ✅ 已创建 |
| 示例数据 | ✅ 包含 | ❌ 空的 |
| uploads文件 | ✅ 保留（40个文件） | ❌ 清空 |
| 可立即查看NFT | ✅ 是 | ❌ 否 |
| 适合学习 | ✅ 是 | ❌ 否 |
| 适合开发 | ✅ 是 | ✅ 是 |

---

**或者让JPA自动创建（仅适用于方式B）：**
- 跳过手动执行SQL脚本
- 配置文件中已设置 `ddl-auto: update`
- 首次启动后端时会自动创建表（但不会有数据）

---

### 步骤3：配置后端

#### 3.1 修改数据库配置

编辑 `D:\brokerwallet\BrokerWallet-backend\src\main\resources\application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brokerwallet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: 
    password:   # ⚠️ 修改为您的MySQL密码
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 3.2 修改区块链配置

编辑 `D:\brokerwallet\BrokerWallet-backend\blockchain-config.yml`：

```yaml
blockchain:
  # 节点RPC地址（⚠️ 修改为现在运行的节点地址）
  rpc-url: "http://127.0.0.1:56873"
  
  # 账户地址（⚠️ 目前只有这个账户有合约调用权限，当然也可以使用自己的账户重新部署这两个智能合约）
  account-address: "0x8c056ccb92c567da3fee27c23d4f2f107f203879"
  
  # 智能合约地址（⚠️ 修改为您部署的合约地址）
  contracts:
    # 勋章系统合约地址
    medal-contract: "0x1a202bfa10ea97a742ad22fcb1a7913821bf1b18"
    # NFT铸造合约地址
    nft-contract: "0x1bd997AE79DF9453b75b7b8D016a652a9c62E980"
    # 代币合约地址
    token-contract: "0xF5c7A871DE8fa7A3393C528d57A519DcEB275f19"

# 连接超时配置
timeout:
  connect: 60
  read: 600
  write: 600
```

**⚠️ 重要：私钥配置**

由于安全原因，私钥不应该直接写在配置文件中。您需要在代码中配置：

编辑 `src/main/java/com/brokerwallet/config/BlockchainConfig.java`：

```java
@Configuration
public class BlockchainConfig {
    // ⚠️ 在这里配置私钥（仅用于开发环境）
    private static final String PRIVATE_KEY = "114737911582466852051274090883801946221241082675647104558725103141732245958742";
    
    // ... 其他配置
}
```

**⚠️ 生产环境建议：**
- 使用环境变量存储私钥
- 使用密钥管理服务（如AWS KMS）
- 切勿将私钥提交到代码仓库

#### 3.3 配置服务器URL（本地开发）

编辑 `application.yml`：

```yaml
brokerwallet:
  server:
    # 本地开发使用localhost
    url: "http://localhost:5000"
  
  file:
    # 文件存储路径（相对路径，无需修改）
    upload-path: uploads/
```

**⚠️ 重要：关于文件路径配置**

本项目使用**相对路径**存储文件，这意味着：

✅ **可以做的：**
- 移动整个 `BrokerWallet-backend` 文件夹到任何位置
- 在不同电脑上运行（只要目录结构不变）
- 修改项目文件夹名称（如改为 `BrokerWallet-backend-v2`）
- 将项目放在任何盘符（C盘、D盘、E盘等）

❌ **不能做的：**
- 单独移动 `uploads` 文件夹到其他位置
- 修改 `uploads` 文件夹内部的目录结构
- 删除 `uploads` 文件夹中的文件（如果使用示例数据）

**文件路径说明：**

1. **配置文件中的路径：** `uploads/`（相对于项目根目录）
2. **数据库中的路径：** `uploads/proofs/users/6/xxx.pdf`（相对路径）
3. **实际文件位置：** `D:\brokerwallet\BrokerWallet-backend\uploads\proofs\users\6\xxx.pdf`
4. **访问URL：** `http://localhost:5000/uploads/proofs/users/6/xxx.pdf`

**URL拼接逻辑：**
```
完整URL = brokerwallet.server.url + 数据库中的路径
        = http://localhost:5000 + /uploads/proofs/users/6/xxx.pdf
        = http://localhost:5000/uploads/proofs/users/6/xxx.pdf
```

**静态资源映射：**
```java
// WebConfig.java 中的配置
registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:uploads/")  // 相对路径
```

这个配置会自动将 `/uploads/**` 的HTTP请求映射到项目根目录下的 `uploads/` 文件夹。

**⚠️ 如果移动项目位置后出现文件访问问题：**

1. **检查 `uploads` 文件夹是否存在：**
   ```bash
   cd D:\brokerwallet\BrokerWallet-backend
   ls uploads/
   ```

2. **检查后端启动日志：**
   ```
   ✅ Static resource handler configured: /uploads/** -> file:uploads/
   ```

3. **测试文件访问：**
   ```bash
   # 在浏览器访问
   http://localhost:5000/uploads/nft-images/users/6/xxx.jpg
   ```

4. **如果使用示例数据，确保 `uploads` 文件夹完整：**
   ```bash
   # 检查文件数量
   Get-ChildItem uploads -Recurse -File | Measure-Object
   # 应该有40个文件
   ```

---

### 步骤4：启动后端

#### 4.1 使用Maven启动

```bash
cd D:\brokerwallet\BrokerWallet-backend

# 方式1：直接运行
mvn spring-boot:run

# 方式2：使用启动脚本（Windows）
start-server.bat

# 方式3：打包后运行
mvn clean package -DskipTests
java -jar target/brokerwallet-backend-1.0.0.jar
```

#### 4.2 验证后端启动

**查看日志：**
```bash
# 实时查看日志
tail -f logs/brokerwallet-backend.log

# 或直接在控制台查看输出
```

**测试API：**
```bash
# 打开浏览器访问
http://localhost:5000

# 或使用curl测试
curl http://localhost:5000/api/test

# 测试区块链连接
curl http://localhost:5000/api/blockchain/health
```

**预期输出：**
```json
{
  "status": "ok",
  "blockchainConnected": true,
  "accountAddress": "0x8c056ccb92c567da3fee27c23d4f2f107f203879",
  "balance": "1234.56"
}
```

---

### 步骤5：启动前端

#### 5.1 安装依赖

```bash
cd D:\brokerwallet\brokerwallet-frontend

# 安装依赖（首次运行）
npm install
```

#### 5.2 配置后端地址（本地开发）

编辑 `src/config/server.ts`：

```typescript
export const SERVER_CONFIG = {
  // 本地开发
  baseURL: 'http://localhost:5000',
  
  timeout: 30000,
};
```

#### 5.3 启动开发服务器

```bash
# 启动前端
npm run dev
```

**访问前端：**
- 打开浏览器访问：http://localhost:5173
- 应该能看到管理员登录界面

**管理员登录：**
- 用户名：`admin`
- 密码：`admin123`
- 登录后可以进入管理后台界面

> **注意**：后端启动时会自动创建默认的 admin 账户，无需手动执行 SQL 脚本。

**创建新管理员（可选）：**
1. 在登录页面点击"创建新管理员"
2. 填写新管理员信息
3. 使用 admin/admin123 作为验证
4. 创建成功后可以使用新账户登录

---

### 步骤6：运行移动端

#### 6.1 配置服务器地址

编辑 `D:\brokerwallet\brokerwallet-academic\app\src\main\java\com\example\brokerfi\config\ServerConfig.java`：

```java
public class ServerConfig {
    // 本地开发（USB调试）
    // 使用localhost（通过adb reverse）
    public static final String BASE_URL = "http://localhost:5000";
    
    // 或使用电脑内网IP（WiFi调试）
    // public static final String BASE_URL = "http://192.168.1.100:5000";
}
```

#### 6.2 连接手机并配置USB调试

**在手机上：**
1. 进入"设置" → "关于手机"
2. 连续点击"版本号"7次，开启开发者模式
3. 返回"设置" → "开发者选项"
4. 开启"USB调试"
5. 使用USB线连接电脑

**在电脑上：**
```bash
# 验证手机连接
adb devices
# 应该显示：List of devices attached
#          XXXXXXXXXX      device

# 配置端口转发（重要！）
adb reverse tcp:5000 tcp:5000

# 验证端口转发
adb reverse --list
# 应该显示：tcp:5000 tcp:5000
```

#### 6.3 在Android Studio中运行

1. 打开Android Studio
2. 打开项目：`D:\brokerwallet\brokerwallet-academic`
3. 等待Gradle同步完成
4. 选择连接的设备
5. 点击运行按钮（绿色三角形）

**或使用命令行：**
```bash
cd D:\brokerwallet\brokerwallet-academic

# 构建APK
./gradlew assembleDebug

# 安装到手机
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.example.brokerfi/.MainActivity
```

---

## ✅ 验证部署

### 1. 后端验证

```bash
# 健康检查
curl http://localhost:5000/api/test

# 区块链连接
curl http://localhost:5000/api/blockchain/health

# 账户信息
curl http://localhost:5000/api/blockchain/account-info
```

**预期结果：**
- ✅ 所有API返回正常
- ✅ 区块链连接成功
- ✅ 账户余额显示正确

### 2. 前端验证

- ✅ 访问 http://localhost:5173
- ✅ 能看到"材料审核"、"勋章排行"等菜单
- ✅ 点击"账户状态"能看到后端账户信息
- ✅ 能正常加载数据

### 3. 移动端验证

- ✅ 应用能正常启动
- ✅ 能导入钱包账户
- ✅ 能查看勋章排行榜
- ✅ 能提交证明材料
- ✅ 能查看NFT

---

## 🔄 完整测试流程

### 测试场景：提交证明材料并铸造NFT

#### 1. 在移动端提交材料

```
1. 打开应用 → 点击"DAO"
2. 点击"📄 Proof Submit"
3. 选择证明文件（可多选）
4. （可选）选择NFT图片
5. 输入昵称
6. 点击"Submit Proof"
7. 等待上传完成
```

#### 2. 在前端审核材料

```
1. 打开浏览器 → http://localhost:5173
2. 点击"材料审核"
3. 在"待审核"标签页找到刚才的提交
4. 点击批次查看详情
5. 点击"通过"按钮
6. 选择勋章类型（金/银/铜）
7. 输入代币奖励数量
8. 点击"分配勋章"
9. 等待区块链交易完成
```

#### 3. 在移动端查看结果

```
1. 返回移动端
2. 点击"👤 My"
3. 查看"🏆 My Medals" - 应该显示新获得的勋章
4. 查看"📝 Submission History" - 状态应该是"Approved"
5. 查看"🖼️ My NFTs" - 应该显示新铸造的NFT
6. 点击NFT查看详情
```

---

## 🔧 常见问题排查

### Q1: 后端启动失败

**错误：** `Communications link failure`

**原因：** 数据库连接失败

**解决：**
```bash
# 1. 检查MySQL是否启动
net start MySQL80

# 2. 验证密码
mysql -u root -p

# 3. 检查数据库是否存在
USE brokerwallet;

# 4. 检查application.yml中的配置
```

---

### Q2: 区块链连接失败

**错误：** `Connection refused` 或 `Unable to connect to blockchain`

**原因：** 区块链节点未启动或地址错误

**解决：**
```bash
# 1. 检查节点是否运行
curl http://127.0.0.1:56873

# 2. 检查blockchain-config.yml中的rpc-url

# 3. 查看节点日志
```

---

### Q3: 移动端无法连接后端

**错误：** `Connection timeout` 或 `Network error`

**原因：** 端口转发未配置或后端未启动

**解决：**
```bash
# 1. 验证后端是否运行
curl http://localhost:5000/api/test

# 2. 检查adb连接
adb devices

# 3. 重新配置端口转发
adb reverse tcp:5000 tcp:5000

# 4. 验证端口转发
adb reverse --list

# 5. 在手机浏览器测试
# 打开手机浏览器，访问 http://localhost:5000
```

---

### Q4: NFT铸造权限不足

**错误：** `Not authorized to mint NFT`

**原因：** 后端账户没有NFT铸造权限

**解决：**
```bash
# 使用合约部署者账户授予权限
cd contract
npx hardhat run scripts/grant-nft-permission.js --network brokerchain

# 或在前端"账户状态"页面查看权限状态
```

---

### Q5: 文件上传失败

**错误：** `File too large` 或 `File type not allowed`

**原因：** 文件大小或类型限制

**解决：**
编辑 `application.yml`：
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

brokerwallet:
  file:
    max-size: 52428800  # 50MB
    allowed-types:
      - image/jpeg
      - image/png
      - application/pdf
      # 添加更多类型
```

---

### Q6: 前端页面空白

**错误：** 浏览器显示空白页

**原因：** 构建错误或路由配置问题

**解决：**
```bash
# 1. 清除缓存
rm -rf node_modules package-lock.json
npm install

# 2. 重新启动
npm run dev

# 3. 检查浏览器控制台错误
# 按F12打开开发者工具查看错误信息

# 4. 使用无痕模式访问
```

---

## 📊 系统架构图

```
┌─────────────────────────────────────────────────────────┐
│                    BrokerChain 区块链                    │
│  - RPC节点：http://127.0.0.1:56873                      │
│  - 勋章合约：0x1a202bfa...                              │
│  - NFT合约：0x1bd997AE...                               │
└─────────────────────────────────────────────────────────┘
                          ↑
                          │ Web3j
                          │
┌─────────────────────────────────────────────────────────┐
│              BrokerWallet-backend (后端)                │
│  - 端口：5000                                           │
│  - 数据库：MySQL (localhost:3306/brokerwallet)          │
│  - 文件存储：uploads/                                   │
└─────────────────────────────────────────────────────────┘
                    ↑                    ↑
                    │ HTTP API           │ HTTP API
                    │                    │
    ┌───────────────┴─────┐    ┌────────┴──────────┐
    │                     │    │                   │
┌───────────────────┐  ┌──────────────────────┐
│ brokerwallet-     │  │ brokerwallet-        │
│ frontend (前端)   │  │ academic (移动端)    │
│ - 端口：5173      │  │ - USB调试            │
│ - Vue 3 + Vuetify │  │ - adb reverse        │
└───────────────────┘  └──────────────────────┘
```

---

## 📝 配置文件总结

### 需要修改的配置文件

| 文件 | 位置 | 需要修改的内容 |
|------|------|---------------|
| `application.yml` | `BrokerWallet-backend/src/main/resources/` | MySQL密码 |
| `blockchain-config.yml` | `BrokerWallet-backend/` | RPC地址、账户地址、合约地址 |
| `BlockchainConfig.java` | `BrokerWallet-backend/src/main/java/.../config/` | 私钥（代码中） |
| `server.ts` | `brokerwallet-frontend/src/config/` | 后端API地址 |
| `ServerConfig.java` | `brokerwallet-academic/app/src/main/java/.../config/` | 后端API地址 |

---

## 🎯 部署检查清单

部署前确认：

- [ ] MySQL已安装并运行
- [ ] 数据库 `brokerwallet` 已创建
- [ ] 表结构已创建（手动或自动）
- [ ] BrokerChain节点已启动
- [ ] 智能合约已部署
- [ ] `application.yml` 中的MySQL密码已修改
- [ ] `blockchain-config.yml` 中的配置已修改
- [ ] 后端账户有足够的BKC余额
- [ ] 后端账户已被授予NFT铸造权限
- [ ] 后端能正常启动（http://localhost:5000）
- [ ] 前端能正常启动（http://localhost:5173）
- [ ] 手机已连接并开启USB调试
- [ ] `adb reverse tcp:5000 tcp:5000` 已执行
- [ ] 移动端能正常运行

---

## 📚 相关文档

- **项目结构：** `PROJECT_STRUCTURE.md`
- **云部署指南：** `CLOUD_DEPLOYMENT_GUIDE.md`
- **数据库结构：** `database/FINAL_SCHEMA.md`
- **智能合约：** `contracts/README.md`
- **前端文档：** `../brokerwallet-frontend/PROJECT_STRUCTURE.md`
- **移动端文档：** `../brokerwallet-academic/DAO_FEATURE_STRUCTURE.md`

---

## 🆘 获取帮助

遇到问题？

1. **查看日志：** `logs/brokerwallet-backend.log`
2. **检查配置：** 确保所有配置文件正确
3. **验证连接：** 测试数据库和区块链连接
4. **查看控制台：** 检查错误信息

---

**祝您部署顺利！🎉**

如有问题，请参考上述文档或检查日志文件。

