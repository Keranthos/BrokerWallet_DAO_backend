# 管理员认证系统使用指南

## 📋 功能概述

BrokerWallet管理员认证系统提供以下功能：

1. **管理员登录** - 基于用户名密码的身份验证
2. **管理员注册** - 需要已有管理员验证才能创建新管理员
3. **密码加密** - 使用SHA-256哈希存储密码

---

## 🚀 快速开始

### 方式1：数据库自动创建表结构（推荐）

如果使用 `application.yml` 中的 `spring.jpa.hibernate.ddl-auto=update` 配置：

1. **启动后端服务**
```bash
mvn spring-boot:run
```

2. **后端会自动完成以下操作**：
   - 创建 `admins` 表（如果不存在）
   - 检查 `admin` 账户是否存在
   - 如果不存在，自动创建默认管理员账户
     - 用户名: `admin`
     - 密码: `admin123`

3. **启动前端服务**
```bash
cd brokerwallet-frontend
npm install
npm run dev
```

4. **登录系统**

访问 `http://localhost:3000/login`

使用默认账号登录：
- 用户名: `admin`
- 密码: `admin123`

### 方式2：手动执行初始化脚本

如果希望手动控制数据库初始化：

```bash
cd database
mysql -u root -p < init.sql
```

这会创建所有表，并插入默认管理员账户。

---

✅ **简单易用**：
- 后端启动时会自动检查并创建 admin 账户
- admin 账户可以一直使用，密码固定为 admin123
- 可以用 admin 账户创建新的管理员账户

---

## 🔑 管理员操作指南

### 登录系统

1. 访问登录页面：`http://localhost:3000/login`
2. 输入用户名和密码
3. 点击"Sign In"按钮
4. 登录成功后自动跳转到管理员界面

### 创建新管理员

1. 在登录页面点击"创建新管理员"
2. 或直接访问：`http://localhost:3000/register`
3. 填写新管理员信息：
   - 新管理员用户名
   - 新管理员密码
   - 确认密码
4. 填写已有管理员验证信息：
   - 已有管理员用户名
   - 已有管理员密码
5. 点击"创建新管理员"
6. 创建成功后使用新账号登录

### 创建新管理员（可选）

如果需要创建新的管理员账户：

1. 访问：`http://localhost:3000/register`
2. 或在登录页面点击"创建新管理员"
3. 填写新管理员信息（用户名、密码、确认密码）
4. 填写已有管理员验证信息（admin/admin123）
5. 点击"创建新管理员"
6. 创建成功后可以使用新账号登录

---

## 🔐 安全特性

### 密码加密

- 使用SHA-256哈希算法
- 密码不以明文存储
- 每次登录时验证哈希值

### 权限控制

- 只有已登录的管理员才能访问管理界面
- 创建新管理员需要已有管理员验证
- 防止未授权访问

### 账户状态管理

管理员账户有三种状态：
- **ACTIVE** - 激活（可以登录）
- **DISABLED** - 禁用（无法登录）
- **LOCKED** - 锁定（无法登录）

---

## 📊 数据库表结构

### admins表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 管理员ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(255) | 密码哈希（SHA-256） |
| display_name | VARCHAR(100) | 显示名称 |
| email | VARCHAR(100) | 邮箱 |
| role | VARCHAR(20) | 角色（admin, super_admin） |
| status | ENUM | 账户状态（ACTIVE, DISABLED, LOCKED） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| last_login_at | DATETIME | 最后登录时间 |
| created_by | BIGINT | 创建者ID（追溯） |

---

## 🔧 API接口

### 登录接口

**URL**: `POST /api/auth/login`

**请求体**:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**:
```json
{
  "code": 1,
  "success": true,
  "message": "登录成功",
  "token": "uuid-token-string",
  "user": {
    "id": 1,
    "username": "admin",
    "displayName": "默认管理员",
    "role": "admin"
  }
}
```

### 注册接口

**URL**: `POST /api/auth/register`

**请求体**:
```json
{
  "newUsername": "newadmin",
  "newPassword": "newpass123",
  "creatorUsername": "admin",
  "creatorPassword": "admin123"
}
```

**响应**:
```json
{
  "code": 1,
  "success": true,
  "message": "管理员创建成功",
  "admin": {
    "id": 2,
    "username": "newadmin",
    "displayName": "newadmin",
    "role": "admin"
  }
}
```

### 检查初始化状态

**URL**: `GET /api/auth/check-init`

**响应**:
```json
{
  "success": true,
  "hasAdmin": true,
  "needInit": false
}
```

---

## 🛠️ 常见问题

### Q1: 忘记密码怎么办？

**方案1：使用admin账户创建新账户**

admin账户密码是固定的`admin123`，可以一直使用。如果需要新账户，可以用admin账户创建。

**方案2：重置admin密码**

```sql
-- 重置admin账户的密码为admin123
UPDATE admins 
SET password = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE username = 'admin';
```

**方案3：重新初始化数据库**

```bash
# 删除并重建数据库
mysql -u root -p
DROP DATABASE brokerwallet;
CREATE DATABASE brokerwallet;
EXIT;

# 重新执行初始化脚本
mysql -u root -p < init.sql
```

### Q2: 如何生成密码哈希？

使用在线SHA-256工具或以下代码：

**Java**:
```java
MessageDigest digest = MessageDigest.getInstance("SHA-256");
byte[] hash = digest.digest("your-password".getBytes());
StringBuilder hexString = new StringBuilder();
for (byte b : hash) {
    String hex = Integer.toHexString(0xff & b);
    if (hex.length() == 1) hexString.append('0');
    hexString.append(hex);
}
String hashedPassword = hexString.toString();
```

**在线工具**: https://emn178.github.io/online-tools/sha256.html

### Q3: 如何禁用管理员账户？

```sql
-- 禁用账户
UPDATE admins 
SET status = 'DISABLED'
WHERE username = 'admin';

-- 重新激活
UPDATE admins 
SET status = 'ACTIVE'
WHERE username = 'admin';
```

### Q4: 创建新管理员时提示"创建者验证失败"？

检查：
1. 创建者用户名是否正确
2. 创建者密码是否正确
3. 创建者账户是否处于ACTIVE状态

### Q5: 登录后立即被退出？

检查：
1. 后端服务是否正常运行
2. token是否正确保存到localStorage
3. 浏览器控制台是否有错误信息

---

## 🔄 升级建议

### 当前实现（简单版）

- ✅ 基本的用户名密码认证
- ✅ SHA-256密码哈希
- ✅ 简单的UUID token
- ✅ 管理员创建需要验证

### 生产环境建议

1. **使用JWT Token**
   - 更安全的token机制
   - 包含过期时间
   - 可以携带用户信息

2. **使用BCrypt密码哈希**
   - 比SHA-256更安全
   - 自动加盐
   - 防止彩虹表攻击

3. **添加Token刷新机制**
   - Access Token（短期）
   - Refresh Token（长期）

4. **添加登录日志**
   - 记录登录IP
   - 记录登录时间
   - 异常登录检测

5. **添加密码强度要求**
   - 必须包含大小写字母
   - 必须包含数字
   - 必须包含特殊字符

---

## 📝 开发说明

### 添加新的认证方式

1. 在`AdminService`中添加新的验证方法
2. 在`AuthController`中添加新的API接口
3. 在前端创建对应的登录组件

### 修改密码哈希算法

修改`AdminService.hashPassword()`方法：

```java
// 使用BCrypt（推荐）
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

private String hashPassword(String password) {
    return passwordEncoder.encode(password);
}

private boolean verifyPassword(String rawPassword, String hashedPassword) {
    return passwordEncoder.matches(rawPassword, hashedPassword);
}
```

---

## 🎯 最佳实践

1. **admin账户可以一直使用** - 密码固定为admin123，简单易记
2. **可选创建新管理员** - 如果需要，可以用admin账户创建新管理员
3. **定期检查管理员账户** - 确保只有需要的人有管理员权限
4. **生产环境建议** - 可以修改admin密码或创建专用管理员

---

**最后更新**: 2025年10月16日

## 🆕 更新日志

### 2025年10月16日
- ✅ 添加了后端启动时自动初始化 admin 账户的功能
- ✅ 创建了 `AdminInitializer` 组件，在应用启动时检查并创建默认管理员
- ✅ 简化了部署流程，无需手动执行 SQL 脚本即可使用
- ✅ 更新了登录和注册界面，移除了 logo 图标

