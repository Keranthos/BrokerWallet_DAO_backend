# BrokerWallet 云服务器部署配置指南

本指南说明如何将BrokerWallet系统部署到云服务器上，以及需要修改哪些配置文件。

---

## 📋 云服务器要求

### 服务器配置

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 2核 | 4核 |
| 内存 | 4GB | 8GB |
| 存储 | 40GB | 100GB SSD |
| 带宽 | 1Mbps | 5Mbps |
| 操作系统 | Ubuntu 20.04+ | Ubuntu 22.04 LTS |

### 需要开放的端口

| 端口 | 用途 | 说明 |
|------|------|------|
| 22 | SSH | 远程管理 |
| 80 | HTTP | 前端访问（可选） |
| 443 | HTTPS | 前端访问（推荐） |
| 5000 | 后端API | 后端服务 |
| 3306 | MySQL | 数据库（建议仅内网访问） |
| 8545 | 区块链RPC | 区块链节点（如果自建） |

---

## 🔧 需要修改的配置文件

### 1. 后端配置

#### 1.1 `application.yml` - 数据库和服务器配置

**文件位置：** `BrokerWallet-backend/src/main/resources/application.yml`

**需要修改的配置：**

```yaml
server:
  port: 5000  # 保持不变，或根据需要修改

spring:
  datasource:
    # ⚠️ 修改1：数据库连接地址
    # 本地：localhost
    # 云服务器：localhost（如果MySQL在同一服务器）或内网IP
    url: jdbc:mysql://localhost:3306/brokerwallet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    
    username: root
    
    # ⚠️ 修改2：数据库密码（使用强密码）
    password: YOUR_STRONG_PASSWORD
    
    driver-class-name: com.mysql.cj.jdbc.Driver

brokerwallet:
  server:
    # ⚠️ 修改3：服务器URL（重要！）
    # 本地开发：http://localhost:5000
    # 云服务器：http://your-domain.com:5000 或 http://your-server-ip:5000
    # 推荐使用域名，如：http://api.brokerwallet.com
    url: "http://your-domain.com:5000"
  
  file:
    # 文件存储路径（相对路径，无需修改）
    upload-path: uploads/
    
    # 最大文件大小（根据需要调整）
    max-size: 52428800  # 50MB
```

**示例配置（云服务器）：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brokerwallet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: MyStr0ngP@ssw0rd!2024
    driver-class-name: com.mysql.cj.jdbc.Driver

brokerwallet:
  server:
    # 使用域名（推荐）
    url: "https://api.brokerwallet.com"
    # 或使用IP地址
    # url: "http://123.456.789.012:5000"
```

---

#### 1.2 `blockchain-config.yml` - 区块链配置

**文件位置：** `BrokerWallet-backend/blockchain-config.yml`

**需要修改的配置：**

```yaml
blockchain:
  # ⚠️ 修改1：RPC节点地址
  # 本地节点：http://127.0.0.1:8545
  # 云服务器节点：http://localhost:8545（如果节点在同一服务器）
  # 远程节点：http://remote-node-ip:8545
  rpc-url: "http://localhost:8545"
  
  # ⚠️ 修改2：后端账户地址（管理员账户）
  account-address: "0x8c056ccb92c567da3fee27c23d4f2f107f203879"
  
  # ⚠️ 修改3：智能合约地址（部署后的地址）
  contracts:
    # 勋章系统合约地址
    medal-contract: "0x1a202bfa10ea97a742ad22fcb1a7913821bf1b18"
    
    # NFT铸造合约地址
    nft-contract: "0x1bd997AE79DF9453b75b7b8D016a652a9c62E980"
    
    # 代币合约地址
    token-contract: "0xF5c7A871DE8fa7A3393C528d57A519DcEB275f19"

# 连接超时配置（根据网络情况调整）
timeout:
  connect: 60
  read: 600
  write: 600
```

**示例配置（云服务器）：**

```yaml
blockchain:
  # 使用云服务器上的本地节点
  rpc-url: "http://localhost:8545"
  
  # 或使用远程节点
  # rpc-url: "http://blockchain-node.example.com:8545"
  
  account-address: "0x8c056ccb92c567da3fee27c23d4f2f107f203879"
  
  contracts:
    medal-contract: "0x1a202bfa10ea97a742ad22fcb1a7913821bf1b18"
    nft-contract: "0x1bd997AE79DF9453b75b7b8D016a652a9c62E980"
    token-contract: "0xF5c7A871DE8fa7A3393C528d57A519DcEB275f19"
```

---

#### 1.3 `BlockchainConfig.java` - 私钥配置

**文件位置：** `BrokerWallet-backend/src/main/java/com/brokerwallet/config/BlockchainConfig.java`

**⚠️ 重要：私钥管理**

**方式1：代码中配置（仅用于测试）**
```java
@Configuration
public class BlockchainConfig {
    // ⚠️ 不推荐：直接在代码中写私钥
    private static final String PRIVATE_KEY = "114737911582466852051274090883801946221241082675647104558725103141732245958742";
}
```

**方式2：环境变量（推荐）**
```java
@Configuration
public class BlockchainConfig {
    // ✅ 推荐：从环境变量读取
    private String privateKey = System.getenv("BLOCKCHAIN_PRIVATE_KEY");
}
```

**在服务器上设置环境变量：**
```bash
# 编辑 /etc/environment 或 ~/.bashrc
export BLOCKCHAIN_PRIVATE_KEY="114737911582466852051274090883801946221241082675647104558725103141732245958742"

# 重新加载
source ~/.bashrc
```

**方式3：配置文件（推荐用于生产）**
```yaml
# 在 application.yml 中添加
blockchain:
  private-key: ${BLOCKCHAIN_PRIVATE_KEY}
```

然后在启动时传入：
```bash
java -jar -DBLOCKCHAIN_PRIVATE_KEY="your-private-key" brokerwallet-backend.jar
```

---

### 2. 前端配置

#### 2.1 `server.ts` - 后端API地址

**文件位置：** `brokerwallet-frontend/src/config/server.ts`

**需要修改的配置：**

```typescript
export const SERVER_CONFIG = {
  // ⚠️ 修改：后端API地址
  // 本地开发：http://localhost:5000
  // 云服务器：http://your-domain.com:5000 或 http://your-server-ip:5000
  // 推荐使用域名，如：https://api.brokerwallet.com
  baseURL: 'http://your-domain.com:5000',
  
  // 请求超时时间（毫秒）
  timeout: 30000,
};
```

**示例配置（云服务器）：**

```typescript
export const SERVER_CONFIG = {
  // 使用HTTPS域名（推荐）
  baseURL: 'https://api.brokerwallet.com',
  
  // 或使用HTTP + IP地址
  // baseURL: 'http://123.456.789.012:5000',
  
  timeout: 30000,
};
```

---

### 3. 移动端配置

#### 3.1 `ServerConfig.java` - 后端API地址

**文件位置：** `brokerwallet-academic/app/src/main/java/com/example/brokerfi/config/ServerConfig.java`

**需要修改的配置：**

```java
public class ServerConfig {
    // ⚠️ 修改：后端API地址
    // 本地开发（USB调试）：http://localhost:5000
    // 本地开发（WiFi）：http://192.168.1.100:5000
    // 云服务器：http://your-domain.com:5000 或 http://your-server-ip:5000
    public static final String BASE_URL = "http://your-domain.com:5000";
}
```

**示例配置（云服务器）：**

```java
public class ServerConfig {
    // 使用HTTPS域名（推荐）
    public static final String BASE_URL = "https://api.brokerwallet.com";
    
    // 或使用HTTP + IP地址
    // public static final String BASE_URL = "http://123.456.789.012:5000";
}
```

---

## 📝 配置修改总结表

| 配置文件 | 位置 | 需要修改的内容 | 示例值 |
|---------|------|---------------|--------|
| `application.yml` | `BrokerWallet-backend/src/main/resources/` | 数据库密码 | `MyStr0ngP@ssw0rd!2024` |
| `application.yml` | 同上 | `brokerwallet.server.url` | `https://api.brokerwallet.com` |
| `blockchain-config.yml` | `BrokerWallet-backend/` | `rpc-url` | `http://localhost:8545` |
| `blockchain-config.yml` | 同上 | `account-address` | `0x8c056ccb...` |
| `blockchain-config.yml` | 同上 | 合约地址 | `0x1a202bfa...` |
| `BlockchainConfig.java` | `BrokerWallet-backend/src/main/java/.../config/` | 私钥（环境变量） | 使用环境变量 |
| `server.ts` | `brokerwallet-frontend/src/config/` | `baseURL` | `https://api.brokerwallet.com` |
| `ServerConfig.java` | `brokerwallet-academic/app/src/main/java/.../config/` | `BASE_URL` | `https://api.brokerwallet.com` |

---

## 🚀 云服务器部署步骤

### 步骤1：准备服务器环境

```bash
# 1. 连接到服务器
ssh root@your-server-ip

# 2. 更新系统
apt update && apt upgrade -y

# 3. 安装Java 17
apt install openjdk-17-jdk -y
java -version

# 4. 安装MySQL
apt install mysql-server -y
systemctl start mysql
systemctl enable mysql

# 5. 安装Node.js（用于前端）
curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
apt install nodejs -y
node -v
npm -v

# 6. 安装Nginx（用于前端托管和反向代理）
apt install nginx -y
systemctl start nginx
systemctl enable nginx

# 7. 安装Git
apt install git -y
```

---

### 步骤2：配置MySQL

```bash
# 1. 安全配置
mysql_secure_installation

# 2. 创建数据库
mysql -u root -p

# 在MySQL中执行：
CREATE DATABASE brokerwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'brokerwallet'@'localhost' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON brokerwallet.* TO 'brokerwallet'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 3. 导入数据库结构
mysql -u root -p brokerwallet < /path/to/init.sql
```

---

### 步骤3：部署后端

```bash
# 1. 创建应用目录
mkdir -p /opt/brokerwallet
cd /opt/brokerwallet

# 2. 从Git拉取代码（或上传文件）
git clone https://github.com/your-repo/brokerwallet.git
cd brokerwallet/BrokerWallet-backend

# 3. 修改配置文件
nano src/main/resources/application.yml
# 修改数据库密码和服务器URL

nano blockchain-config.yml
# 修改RPC地址和合约地址

# 4. 设置环境变量（私钥）
echo 'export BLOCKCHAIN_PRIVATE_KEY="your-private-key"' >> ~/.bashrc
source ~/.bashrc

# 5. 构建项目
mvn clean package -DskipTests

# 6. 创建systemd服务
nano /etc/systemd/system/brokerwallet-backend.service
```

**systemd服务配置：**
```ini
[Unit]
Description=BrokerWallet Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/brokerwallet/BrokerWallet-backend
ExecStart=/usr/bin/java -jar /opt/brokerwallet/BrokerWallet-backend/target/brokerwallet-backend-1.0.0.jar
Restart=on-failure
RestartSec=10
Environment="BLOCKCHAIN_PRIVATE_KEY=your-private-key"

[Install]
WantedBy=multi-user.target
```

```bash
# 7. 启动服务
systemctl daemon-reload
systemctl start brokerwallet-backend
systemctl enable brokerwallet-backend

# 8. 查看状态
systemctl status brokerwallet-backend

# 9. 查看日志
journalctl -u brokerwallet-backend -f
```

---

### 步骤4：部署前端

```bash
# 1. 进入前端目录
cd /opt/brokerwallet/brokerwallet-frontend

# 2. 修改配置
nano src/config/server.ts
# 修改 baseURL 为云服务器地址

# 3. 安装依赖
npm install

# 4. 构建生产版本
npm run build

# 5. 配置Nginx
nano /etc/nginx/sites-available/brokerwallet-frontend
```

**Nginx配置：**
```nginx
server {
    listen 80;
    server_name your-domain.com;  # 修改为您的域名

    root /opt/brokerwallet/brokerwallet-frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # API反向代理（可选）
    location /api/ {
        proxy_pass http://localhost:5000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# 6. 启用配置
ln -s /etc/nginx/sites-available/brokerwallet-frontend /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

---

### 步骤5：配置HTTPS（推荐）

```bash
# 1. 安装Certbot
apt install certbot python3-certbot-nginx -y

# 2. 获取SSL证书
certbot --nginx -d your-domain.com

# 3. 自动续期
certbot renew --dry-run
```

---

### 步骤6：配置防火墙

```bash
# 使用ufw（Ubuntu防火墙）
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 5000/tcp  # 后端API
ufw enable

# 查看状态
ufw status
```

---

## 🔒 安全建议

### 1. 数据库安全

```bash
# 1. 使用强密码
# 2. 限制远程访问
nano /etc/mysql/mysql.conf.d/mysqld.cnf
# 确保 bind-address = 127.0.0.1

# 3. 定期备份
crontab -e
# 添加：0 2 * * * mysqldump -u root -p'password' brokerwallet > /backup/brokerwallet_$(date +\%Y\%m\%d).sql
```

### 2. 私钥安全

```bash
# 1. 使用环境变量或密钥管理服务
# 2. 限制文件权限
chmod 600 ~/.bashrc

# 3. 不要将私钥提交到Git
echo "blockchain-config.yml" >> .gitignore
```

### 3. 服务器安全

```bash
# 1. 禁用root SSH登录
nano /etc/ssh/sshd_config
# PermitRootLogin no

# 2. 使用SSH密钥认证
# 3. 定期更新系统
apt update && apt upgrade -y

# 4. 安装fail2ban防止暴力破解
apt install fail2ban -y
systemctl enable fail2ban
```

---

## 📊 监控和日志

### 后端日志

```bash
# 查看实时日志
journalctl -u brokerwallet-backend -f

# 查看最近100行
journalctl -u brokerwallet-backend -n 100

# 查看应用日志
tail -f /opt/brokerwallet/BrokerWallet-backend/logs/brokerwallet-backend.log
```

### 前端日志

```bash
# Nginx访问日志
tail -f /var/log/nginx/access.log

# Nginx错误日志
tail -f /var/log/nginx/error.log
```

### 系统监控

```bash
# 查看系统资源
htop

# 查看磁盘使用
df -h

# 查看内存使用
free -h

# 查看端口监听
netstat -tulpn | grep LISTEN
```

---

## 🔄 更新部署

### 更新后端

```bash
# 1. 拉取最新代码
cd /opt/brokerwallet/BrokerWallet-backend
git pull

# 2. 重新构建
mvn clean package -DskipTests

# 3. 重启服务
systemctl restart brokerwallet-backend

# 4. 查看状态
systemctl status brokerwallet-backend
```

### 更新前端

```bash
# 1. 拉取最新代码
cd /opt/brokerwallet/brokerwallet-frontend
git pull

# 2. 重新构建
npm install
npm run build

# 3. 重启Nginx
systemctl reload nginx
```

---

## ✅ 部署验证

### 验证后端

```bash
# 1. 健康检查
curl http://localhost:5000/api/test

# 2. 区块链连接
curl http://localhost:5000/api/blockchain/health

# 3. 从外部访问
curl http://your-domain.com:5000/api/test
```

### 验证前端

```bash
# 1. 访问前端
curl http://your-domain.com

# 2. 检查Nginx状态
systemctl status nginx

# 3. 在浏览器访问
# http://your-domain.com 或 https://your-domain.com
```

---

## 🆘 故障排查

### 后端无法启动

```bash
# 查看日志
journalctl -u brokerwallet-backend -n 100

# 检查端口占用
netstat -tulpn | grep 5000

# 检查配置文件
cat src/main/resources/application.yml
```

### 前端无法访问

```bash
# 检查Nginx状态
systemctl status nginx

# 测试Nginx配置
nginx -t

# 查看错误日志
tail -f /var/log/nginx/error.log
```

### 数据库连接失败

```bash
# 检查MySQL状态
systemctl status mysql

# 测试连接
mysql -u brokerwallet -p

# 查看MySQL日志
tail -f /var/log/mysql/error.log
```

---

## 📚 相关文档

- **本地部署指南：** `LOCAL_DEPLOYMENT_GUIDE.md`
- **项目结构：** `PROJECT_STRUCTURE.md`
- **数据库结构：** `database/FINAL_SCHEMA.md`
- **智能合约：** `contracts/README.md`

---

## 📝 配置文件快速参考

### 需要修改的关键配置

```yaml
# application.yml
spring:
  datasource:
    password: YOUR_STRONG_PASSWORD  # ⚠️ 修改
brokerwallet:
  server:
    url: "https://api.brokerwallet.com"  # ⚠️ 修改

# blockchain-config.yml
blockchain:
  rpc-url: "http://localhost:8545"  # ⚠️ 根据实际情况修改
  account-address: "0x..."  # ⚠️ 修改
  contracts:
    medal-contract: "0x..."  # ⚠️ 修改
    nft-contract: "0x..."  # ⚠️ 修改
```

```typescript
// server.ts
export const SERVER_CONFIG = {
  baseURL: 'https://api.brokerwallet.com',  // ⚠️ 修改
};
```

```java
// ServerConfig.java
public static final String BASE_URL = "https://api.brokerwallet.com";  // ⚠️ 修改
```

---

**祝您部署顺利！🎉**

如有问题，请参考相关文档或查看日志文件。

