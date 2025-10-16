# 🔐 后端API鉴权接口说明文档

## 📋 文档概述

本文档详细说明了BrokerWallet后端的API鉴权机制，包括哪些接口需要认证、如何实现鉴权、以及如何正确调用需要认证的接口。

---

## 🎯 鉴权机制概述

### 认证方式

本系统采用 **Session + Cookie** 的认证方式：

1. **登录** → 后端创建Session并返回SessionID（通过Cookie）
2. **后续请求** → 浏览器自动携带Cookie中的SessionID
3. **拦截器验证** → 后端拦截器自动验证Session有效性
4. **放行/拒绝** → 有效则放行，无效则返回401

### 技术实现

- **拦截器**：`AdminAuthInterceptor` 统一拦截 `/api/admin/**`
- **Session管理**：Spring Boot内置Session机制
- **超时时间**：30分钟无操作自动过期
- **跨域支持**：CORS配置允许携带Cookie

---

## 📚 API接口分类

### 1️⃣ 公开接口（无需认证）

这些接口可以在未登录状态下访问：

#### 系统基础接口

| 接口路径 | 方法 | 说明 | 返回示例 |
|---------|------|------|---------|
| `/api/health` | GET | 健康检查 | `{"status": "UP"}` |
| `/api/test` | GET | 测试连接 | `{"message": "success"}` |
| `/api/server/info` | GET | 服务器信息 | 服务器基本信息 |

#### 认证相关接口

| 接口路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/auth/login` | POST | 管理员登录 | `{username, password}` |
| `/api/auth/register` | POST | 创建新管理员 | `{newUsername, newPassword, creatorUsername, creatorPassword}` |
| `/api/auth/logout` | POST | 登出 | 无 |
| `/api/auth/current` | GET | 获取当前管理员信息 | 无 |
| `/api/auth/check-init` | GET | 检查是否需要初始化 | 无 |
| `/api/auth/ensure-admin` | POST | 确保默认admin存在 | 无 |

#### 用户端接口（勋章排行榜等）

| 接口路径 | 方法 | 说明 |
|---------|------|------|
| `/api/medal/ranking` | GET | 获取勋章排行榜 |
| `/api/medal/user-rank/{address}` | GET | 获取用户排名 |
| `/api/medal/stats` | GET | 获取勋章统计 |
| `/api/upload/test` | GET | 测试上传连接 |
| `/api/upload/complete` | POST | 用户提交材料和NFT |
| `/api/upload/user/submissions` | GET | 获取用户提交历史 |

---

### 2️⃣ 管理员接口（需要认证）

⚠️ **这些接口都需要先登录才能访问，否则返回401错误**

#### 用户管理接口

| 接口路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/admin/pending-users` | GET | 获取待审核用户列表 | `page, limit` |
| `/api/admin/approved-users` | GET | 获取已审核用户列表 | `page, limit` |
| `/api/admin/all-users` | GET | 获取所有用户列表 | `page, limit` |
| `/api/admin/material-detail/{id}` | GET | 获取材料详情 | `id` (路径参数) |
| `/api/admin/search-by-display-name` | GET | 按花名搜索用户 | `displayName` |

#### 审核操作接口

| 接口路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/admin/review` | POST | 审核用户并分配勋章 | `{username, approve, firstnum, secondnum, thirdnum}` |
| `/api/admin/update-proof-status` | POST | 更新证明文件状态 | `{proofFileId, status, rejectReason?}` |

#### NFT和区块链接口

| 接口路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/blockchain/nft/mint` | POST | 铸造NFT | `{ownerAddress, name, description, imageData, attributes, nftImageId?}` |
| `/api/admin/generate-default-nft-image` | POST | 生成默认样式NFT图片 | NFT元数据 |

#### 奖励管理接口

| 接口路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/admin/transfer-reward` | POST | 发放代币奖励 | `{toAddress, amount}` |
| `/api/admin/save-token-reward` | POST | 保存代币奖励记录 | `{proofFileId, tokenReward, txHash}` |

#### 系统管理接口

| 接口路径 | 方法 | 说明 | 参数 |
|---------|------|------|------|
| `/api/admin/stats` | GET | 获取审核统计信息 | 无 |
| `/api/admin/account-status` | GET | 检查后端账户状态 | 无 |
| `/api/admin/download/{objectKey}` | GET | 下载文件 | `objectKey` (路径参数) |

---

## 🔒 鉴权实现细节

### 后端实现

#### 1. 拦截器 (`AdminAuthInterceptor.java`)

```java
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private static final String ADMIN_SESSION_KEY = "ADMIN_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        // 获取Session
        HttpSession session = request.getSession(false);
        
        // 验证Session中是否有管理员信息
        if (session == null || session.getAttribute(ADMIN_SESSION_KEY) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"未登录或登录已过期，请重新登录\"}"
            );
            return false; // 拦截请求
        }
        
        return true; // 放行请求
    }
}
```

#### 2. 拦截器配置 (`WebConfig.java`)

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")      // 拦截所有管理员接口
                .excludePathPatterns(                   // 排除认证相关接口
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/check-init",
                        "/api/auth/ensure-admin"
                );
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                .allowCredentials(true)  // 🔑 关键：允许携带Cookie
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

#### 3. 登录接口 (`AuthController.java`)

```java
@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(
        @RequestBody Map<String, String> request, 
        HttpSession session) {
    
    // 验证用户名密码
    Admin admin = adminService.login(username, password);
    
    if (admin != null) {
        // 将管理员信息存入Session
        Map<String, Object> adminInfo = Map.of(
            "id", admin.getId(),
            "username", admin.getUsername(),
            "role", admin.getRole()
        );
        session.setAttribute("ADMIN_USER", adminInfo);
        
        // 设置Session超时（30分钟）
        session.setMaxInactiveInterval(30 * 60);
        
        return ResponseEntity.ok(Map.of(
            "code", 1,
            "success", true,
            "user", adminInfo
        ));
    }
    
    return ResponseEntity.status(401).body(Map.of(
        "success", false,
        "message", "用户名或密码错误"
    ));
}
```

---

## 📡 前端调用示例

### 1. 配置axios客户端

```typescript
// api/index.ts
const apiClient = axios.create({
  baseURL: 'http://localhost:5000',
  withCredentials: true,  // 🔑 关键：自动携带Cookie
});

// 响应拦截器 - 处理401错误
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 未登录或Session过期，跳转登录页
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

### 2. 登录示例

```typescript
// 登录
const response = await fetch('http://localhost:5000/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',  // 🔑 关键：保存Cookie
  body: JSON.stringify({
    username: 'admin',
    password: 'admin123'
  })
});

const data = await response.json();
if (data.success) {
  console.log('登录成功', data.user);
  // Cookie会自动保存，后续请求自动携带
}
```

### 3. 调用管理员接口

```typescript
// 使用axios（自动携带Cookie）
const response = await apiClient.get('/api/admin/pending-users', {
  params: { page: 1, limit: 10 }
});

// 或使用fetch
const response = await fetch('http://localhost:5000/api/admin/pending-users?page=1&limit=10', {
  credentials: 'include'  // 🔑 携带Cookie
});
```

### 4. 登出

```typescript
await fetch('http://localhost:5000/api/auth/logout', {
  method: 'POST',
  credentials: 'include'
});

// 清除本地状态
localStorage.removeItem('user');
window.location.href = '/login';
```

---

## 🧪 测试鉴权

### 使用curl测试

#### 1. 测试未授权访问（应返回401）

```bash
curl http://localhost:5000/api/admin/pending-users
# 预期响应：{"success":false,"message":"未登录或登录已过期，请重新登录"}
```

#### 2. 测试登录并访问

```bash
# 登录并保存Cookie
curl -c cookies.txt -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用Cookie访问管理员接口
curl -b cookies.txt http://localhost:5000/api/admin/pending-users
# 预期：成功返回数据
```

### 使用Postman测试

1. **登录**
   - 方法：POST
   - URL：`http://localhost:5000/api/auth/login`
   - Body：`{"username": "admin", "password": "admin123"}`
   - 查看Cookies标签，应该能看到 `JSESSIONID`

2. **访问管理员接口**
   - 方法：GET
   - URL：`http://localhost:5000/api/admin/stats`
   - Cookie会自动携带
   - 应该能成功获取数据

3. **测试未登录**
   - 清除Cookies
   - 再次访问管理员接口
   - 应返回401错误

---

## 🔧 常见问题

### Q1: 为什么我的请求总是返回401？

**可能原因**：
1. 未登录或Session已过期
2. 前端未配置 `withCredentials: true`
3. 后端CORS未配置 `allowCredentials(true)`
4. Cookie被浏览器阻止（检查跨域设置）

**解决方案**：
```typescript
// 前端axios配置
axios.create({
  withCredentials: true  // ✅ 必须设置
});

// 或fetch配置
fetch(url, {
  credentials: 'include'  // ✅ 必须设置
});
```

### Q2: 登录后为什么看不到Cookie？

**检查**：
1. 浏览器开发者工具 → Application → Cookies
2. 查找 `JSESSIONID`
3. 确认Domain和Path是否正确

**注意**：
- Cookie的Domain必须与后端地址匹配
- 开发环境通常是 `localhost`

### Q3: Session多久会过期？

**默认设置**：30分钟无操作自动过期

可以在 `AuthController.java` 中修改：
```java
session.setMaxInactiveInterval(30 * 60);  // 30分钟
```

### Q4: 如何在生产环境使用？

**建议**：
1. **使用Redis存储Session**（避免重启丢失）
2. **启用HTTPS**（保护Cookie安全）
3. **配置正确的CORS白名单**
4. **设置Session Cookie的 Secure 和 HttpOnly 标志**

---

## 📋 完整接口权限清单

### ✅ 无需认证的接口（17个）

```
GET  /api/health
GET  /api/test
GET  /api/server/info
POST /api/auth/login
POST /api/auth/register
POST /api/auth/logout
GET  /api/auth/current
GET  /api/auth/check-init
POST /api/auth/ensure-admin
GET  /api/medal/ranking
GET  /api/medal/user-rank/{address}
GET  /api/medal/stats
GET  /api/upload/test
POST /api/upload/complete
GET  /api/upload/user/submissions
GET  /api/upload/submission/detail/{id}
GET  /api/blockchain/nft/user/{address}
GET  /api/blockchain/nft/all
GET  /api/blockchain/medals/{address}
GET  /api/blockchain/global-stats
GET  /api/blockchain/health
GET  /api/admin/nft-image/{imageName}        # NFT图片访问（Web管理员端使用）
GET  /api/admin/nft-thumbnail/{imageName}    # NFT缩略图访问（Web管理员端使用）
GET  /api/admin/user/info/{walletAddress}   # 用户信息查询（手机端需要）
GET  /uploads/**                             # 静态文件访问（手机端NFT图片直接访问）
```

### 🔒 需要认证的接口（管理员专用）

```
# 用户管理（5个）
GET  /api/admin/pending-users
GET  /api/admin/approved-users
GET  /api/admin/all-users
GET  /api/admin/material-detail/{id}
GET  /api/admin/search-by-display-name

# 审核操作（2个）
POST /api/admin/review
POST /api/admin/update-proof-status

# NFT管理（6个）
POST /api/blockchain/nft/mint                   # NFT铸造
GET  /api/blockchain/check-nft-permission       # 检查NFT权限
GET  /api/blockchain/nft/check-permission       # 检查铸造权限
GET  /api/blockchain/nft/mint-fee               # 查询铸造费用
GET  /api/blockchain/test-contract              # 测试合约连接
POST /api/admin/generate-default-nft-image      # 生成默认NFT图片

# 奖励管理（2个）
POST /api/admin/transfer-reward
POST /api/admin/save-token-reward

# 系统管理（3个）
GET  /api/admin/stats
GET  /api/admin/account-status
GET  /api/admin/download/{objectKey}
```

**总计**：18个受保护的管理员接口

---

## 🎯 最佳实践

1. **前端**
   - 统一使用axios实例，配置 `withCredentials: true`
   - 在响应拦截器中处理401错误
   - 不要在localStorage中存储敏感信息

2. **后端**
   - 所有管理员接口都应该在 `/api/admin/**` 路径下
   - 不要在拦截器外做二次认证判断
   - 使用统一的错误响应格式

3. **安全**
   - 生产环境使用HTTPS
   - 定期更新管理员密码
   - 使用更强的密码哈希算法（如BCrypt）
   - 考虑添加验证码防止暴力破解

---

## 📖 相关文档

- [管理员认证系统指南](./ADMIN_AUTH_GUIDE.md) - 管理员账户管理和操作指南
- [安全认证实现文档](./SECURITY_AUTH_IMPLEMENTATION.md) - 详细的技术实现说明
- [本地部署指南](./LOCAL_DEPLOYMENT_GUIDE.md) - 包含管理员登录说明

---

**最后更新**：2025年10月16日  
**版本**：v1.0.0

