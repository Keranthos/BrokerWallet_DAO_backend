# BrokerWallet 后端服务

## 📱 项目简介

BrokerWallet后端服务是为BrokerWallet勋章系统提供的本地服务器解决方案。当您暂时没有云端服务器时，可以将电脑作为服务器，通过USB连接手机，为BrokerWallet应用提供以下功能：

- 🏆 **证明材料上传**：支持用户上传各种格式的证明文件
- 🎨 **NFT图片铸造**：支持用户上传图片并进行NFT铸造
- 💾 **数据库管理**：使用MySQL数据库存储文件信息和用户数据
- 📁 **文件系统管理**：本地文件系统存储和管理上传的文件

## 🚀 快速开始

### 环境要求

- ☕ **Java 17** 或更高版本
- 🗄️ **MySQL 8.0** 或更高版本
- 🔧 **Maven 3.6** 或更高版本
- 🖥️ **Windows 10/11** (其他操作系统也支持)

### 安装步骤

#### 1. 准备数据库

1. 启动MySQL服务
2. 使用Navicat或命令行连接MySQL
3. 执行数据库初始化脚本：

```sql
-- 在MySQL中执行
source C:/Users/wanweijie/Desktop/BrokerWallet-backend/database/init.sql
```

或者直接在Navicat中打开并执行 `database/init.sql` 文件。

#### 2. 配置数据库连接

编辑 `src/main/resources/application.yml` 文件，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/brokerwallet?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的MySQL密码  # 请修改为您的实际密码
```

#### 3. 启动服务

在项目根目录下执行：

```bash
# 使用Maven启动
mvn spring-boot:run

# 或者先编译再运行
mvn clean package
java -jar target/backend-1.0.0.jar
```

#### 4. 验证服务

服务启动后，访问以下地址验证：

- 🏥 **健康检查**: http://localhost:5000/api/health
- 📋 **服务器信息**: http://localhost:5000/api/server/info
- 📖 **API文档**: http://localhost:5000/api/docs

## 📱 Android应用配置

### 修改服务器地址

在Android项目中找到 `ServerConfig.java` 文件，修改服务器地址：

```java
// 文件位置: app/src/main/java/com/example/brokerfi/config/ServerConfig.java

public static final String SERVER_HOST = "你的电脑IP地址"; // 例如: "192.168.1.100"
public static final int SERVER_PORT = 5000;
```

### 获取电脑IP地址

#### Windows系统：
```cmd
ipconfig
```
查找 "无线局域网适配器 WLAN" 或 "以太网适配器" 下的 IPv4 地址。

#### 或者查看服务器信息：
访问 http://localhost:5000/api/server/info 获取服务器IP地址。

### USB连接配置

1. 📱 **启用USB调试**：在Android手机的开发者选项中启用USB调试
2. 🔌 **连接USB线**：使用USB线连接手机和电脑
3. 🌐 **端口转发**（如果需要）：
   ```bash
   adb forward tcp:5000 tcp:5000
   ```

## 🔧 API接口文档

### 证明文件相关

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/proof/upload` | POST | 上传证明文件 |
| `/api/proof/list` | GET | 获取证明文件列表 |
| `/api/proof/detail/{id}` | GET | 获取证明文件详情 |
| `/api/proof/download/{id}` | GET | 下载证明文件 |
| `/api/proof/delete/{id}` | DELETE | 删除证明文件 |
| `/api/proof/search` | GET | 搜索证明文件 |
| `/api/proof/statistics` | GET | 获取文件统计信息 |

### NFT图片相关

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/nft/upload` | POST | 上传NFT图片 |
| `/api/nft/mint` | POST | NFT铸造 |
| `/api/nft/list` | GET | 获取NFT图片列表 |
| `/api/nft/detail/{id}` | GET | 获取NFT图片详情 |
| `/api/nft/view/{id}` | GET | 查看NFT图片 |
| `/api/nft/statistics` | GET | 获取NFT统计信息 |

### 系统相关

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/health` | GET | 系统健康检查 |
| `/api/server/info` | GET | 获取服务器信息 |
| `/api/test` | GET | 测试连接 |
| `/api/docs` | GET | 获取API文档 |

## 💡 使用示例

### 上传证明文件

```bash
curl -X POST \
  -F "file=@证明文件.pdf" \
  -F "userId=user123" \
  http://localhost:5000/api/proof/upload
```

### 上传NFT图片

```bash
curl -X POST \
  -F "file=@图片.jpg" \
  -F "nftName=我的NFT" \
  -F "nftDescription=这是我的第一个NFT" \
  -F "userId=user123" \
  http://localhost:5000/api/nft/upload
```

### NFT铸造

```bash
curl -X POST \
  -d "imageId=1&shares=100" \
  http://localhost:5000/api/nft/mint
```

## 📁 文件存储结构

```
uploads/
├── proofs/           # 证明文件
│   └── 2024/01/15/   # 按日期分类
├── nft-images/       # NFT图片
│   └── 2024/01/15/   # 按日期分类
└── thumbnails/       # 缩略图
    └── 2024/01/15/   # 按日期分类
```

## 🗄️ 数据库表结构

### proof_files（证明文件表）
- `id`: 主键
- `file_name`: 文件名
- `original_name`: 原始文件名
- `file_type`: 文件类型
- `file_size`: 文件大小
- `file_path`: 存储路径
- `upload_time`: 上传时间
- `user_id`: 用户ID
- `status`: 文件状态

### nft_images（NFT图片表）
- `id`: 主键
- `image_name`: 图片名
- `nft_name`: NFT名称
- `nft_description`: NFT描述
- `image_type`: 图片类型
- `image_size`: 图片大小
- `mint_status`: 铸造状态
- `transaction_hash`: 交易哈希
- `token_id`: Token ID

## 🔍 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查MySQL服务是否启动
   - 验证数据库连接信息是否正确
   - 确认数据库已创建

2. **文件上传失败**
   - 检查uploads目录权限
   - 验证文件大小是否超过限制（50MB）
   - 确认文件类型是否支持

3. **手机无法连接**
   - 确认电脑和手机在同一网络
   - 检查防火墙设置
   - 验证IP地址配置

4. **端口被占用**
   ```bash
   # Windows查看端口占用
   netstat -ano | findstr :5000
   
   # 结束占用进程
   taskkill /PID <进程ID> /F
   ```

### 日志查看

- 应用日志位置：`logs/brokerwallet-backend.log`
- 控制台实时日志：服务启动时可以看到
- 日志级别：DEBUG（开发环境）

## 🛠️ 开发说明

### 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/brokerwallet/
│   │       ├── config/          # 配置类
│   │       ├── controller/      # 控制器
│   │       ├── entity/          # 实体类
│   │       ├── repository/      # 数据访问层
│   │       ├── service/         # 服务层
│   │       └── util/            # 工具类
│   └── resources/
│       ├── application.yml      # 配置文件
│       └── static/             # 静态资源
└── test/                       # 测试代码
```

### 技术栈

- **Spring Boot 3.2.0**: Web框架
- **Spring Data JPA**: 数据访问
- **MySQL 8.0**: 数据库
- **Maven**: 项目管理
- **SLF4J + Logback**: 日志框架

## 📄 许可证

本项目采用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目！

## 📞 支持

如果您在使用过程中遇到问题，请：

1. 查看本文档的故障排除部分
2. 检查日志文件获取详细错误信息
3. 访问 http://localhost:5000/api/health 检查服务状态
4. 提交Issue描述问题详情

---

🎉 **祝您使用愉快！** 如果这个项目对您有帮助，请给个⭐️支持一下！

