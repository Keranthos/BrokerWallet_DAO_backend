# 🔒 后端安全认证实现文档

## 📋 概述

本文档详细说明了BrokerWallet后端管理员认证与授权系统的实现。该系统基于**Session + 拦截器**的架构，有效防止未授权访问管理员API。

---

## 🎯 解决的安全问题

### ❌ 修复前的安全隐患

1. **前端登录形同虚设**：任何人只要知道API地址，就可以绕过前端直接调用后端管理员接口
2. **数据可被随意篡改**：攻击者可以使用Postman等工具直接调用管理员接口
3. **NFT可被恶意铸造**：没有权限验证的NFT铸造接口

### ✅ 修复后的安全机制

1. **强制Session验证**：所有管理员API都需要有效的Session
2. **统一拦截器**：自动拦截未授权请求，返回401错误
3. **前后端协同**：前端自动处理401错误并跳转登录页
4. **Session自动过期**：30分钟无操作自动登出

---

## 🏗️ 架构设计

### 1. 认证流程图

```
用户登录
   ↓
输入用户名/密码
   ↓
后端验证凭据
   ↓
创建Session（存储管理员信息）
   ↓
返回成功（Cookie自动保存SessionID）
   ↓
后续请求自动携带Cookie
   ↓
拦截器验证Session
   ↓
[有效] → 放行请求
[无效] → 返回401 → 前端跳转登录
```

### 2. 核心组件

#### 后端组件

| 组件 | 文件路径 | 职责 |
|------|---------|------|
| **认证拦截器** | `interceptor/AdminAuthInterceptor.java` | 拦截所有管理员API，验证Session |
| **Web配置** | `config/WebConfig.java` | 配置拦截器规则和CORS |
| **认证控制器** | `controller/AuthController.java` | 处理登录、登出、注册 |
| **管理员服务** | `service/AdminService.java` | 管理员业务逻辑和密码验证 |

#### 前端组件

| 组件 | 文件路径 | 职责 |
|------|---------|------|
| **API客户端** | `api/index.ts` | 配置axios携带Cookie和处理401错误 |
| **登录页面** | `views/authentication/auth/LoginPage.vue` | 管理员登录界面 |
| **认证Store** | `stores/auth.ts` | 管理用户状态和登出逻辑 |

---

## 🔧 实现细节

### 1. 后端拦截器

**文件**：`com.brokerwallet.interceptor.AdminAuthInterceptor`

```java
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private static final String ADMIN_SESSION_KEY = "ADMIN_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        // 处理OPTIONS预检请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取Session
        HttpSession session = request.getSession(false);
        
        // 检查Session是否存在以及是否包含管理员信息
        if (session == null || session.getAttribute(ADMIN_SESSION_KEY) == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"message\":\"未登录或登录已过期，请重新登录\"}"
            );
            return false;
        }

        return true;
    }
}
```

**关键点**：
- 检查Session中是否存在`ADMIN_USER`键
- 未通过验证返回401状态码
- 支持OPTIONS预检请求（CORS）

---

### 2. 拦截器配置

**文件**：`com.brokerwallet.config.WebConfig`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)  // 🔑 关键：允许携带Cookie
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                // 拦截所有管理员接口
                .addPathPatterns("/api/admin/**")
                // 排除不需要认证的路径
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/check-init",
                        "/api/auth/ensure-admin"
                );
    }
}
```

**拦截规则**：
- ✅ **拦截**：`/api/admin/**` 所有管理员接口
- ❌ **放行**：登录、注册、初始化接口

---

### 3. 登录接口（创建Session）

**文件**：`com.brokerwallet.controller.AuthController`

```java
@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(
        @RequestBody Map<String, String> request, 
        HttpSession session) {
    
    // ... 验证用户名密码 ...
    
    Admin admin = adminService.login(username, password);
    
    if (admin != null) {
        // 将管理员信息存入Session
        Map<String, Object> adminInfo = Map.of(
            "id", admin.getId(),
            "username", admin.getUsername(),
            "displayName", admin.getDisplayName(),
            "role", admin.getRole()
        );
        session.setAttribute("ADMIN_USER", adminInfo);
        
        // 设置Session超时时间（30分钟）
        session.setMaxInactiveInterval(30 * 60);
        
        return ResponseEntity.ok(Map.of(
            "code", 1,
            "success", true,
            "user", adminInfo
        ));
    }
    
    // ... 错误处理 ...
}
```

**关键点**：
- 登录成功后创建Session并存储管理员信息
- Session ID通过Cookie自动返回给前端
- 设置30分钟超时

---

### 4. 登出接口（销毁Session）

```java
@PostMapping("/logout")
public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
    session.invalidate();  // 销毁Session
    
    return ResponseEntity.ok(Map.of(
        "code", 1,
        "success", true,
        "message", "登出成功"
    ));
}
```

---

### 5. 前端配置（携带Cookie）

**文件**：`brokerwallet-frontend/src/api/index.ts`

```typescript
// 创建axios实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,  // 🔑 关键：允许携带Cookie
});

// 响应拦截器 - 处理401未授权错误
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Session过期或未登录，清除本地存储并跳转到登录页
      console.warn('会话已过期或未登录，正在跳转到登录页...');
      localStorage.removeItem('user');
      
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

**关键点**：
- `withCredentials: true` 允许携带和保存Cookie
- 自动处理401错误并跳转登录页

---

### 6. 前端登录逻辑

**文件**：`brokerwallet-frontend/src/views/authentication/auth/LoginPage.vue`

```typescript
const handleLogin = async () => {
  try {
    const res = await fetch('http://localhost:5000/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',  // 🔑 关键：携带和保存Cookie
      body: JSON.stringify({
        username: username.value,
        password: password.value
      })
    });

    const data = await res.json();

    if (data.code === 1 && data.success) {
      // Session由Cookie自动管理，只需保存用户基本信息
      auth.loginSuccess({
        user: {
          id: data.user.id,
          name: data.user.displayName || data.user.username,
          role: data.user.role
        }
      });
    }
  } catch (err) {
    error.value = '网络或服务器错误';
  }
};
```

---

## 🔐 安全特性

### 1. Session管理

| 特性 | 说明 |
|------|------|
| **存储方式** | 服务器内存（Spring Boot默认） |
| **超时时间** | 30分钟无操作自动过期 |
| **Session ID** | 通过HttpOnly Cookie传输（防止XSS攻击） |
| **跨域支持** | CORS配置允许前端携带Cookie |

### 2. 密码安全

- 使用SHA-256哈希存储密码
- 登录失败不泄露具体原因（统一返回"用户名或密码错误"）

### 3. 拦截器保护

- 所有`/api/admin/**`接口自动受保护
- 无需在每个Controller方法中重复验证
- 统一的401错误响应格式

---

## 📊 API接口清单

### 认证相关（无需Session）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 管理员登录 |
| `/api/auth/register` | POST | 创建新管理员 |
| `/api/auth/logout` | POST | 登出 |
| `/api/auth/current` | GET | 获取当前管理员信息 |

### 管理员功能（需要Session）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/**` | * | 所有管理员接口（受拦截器保护） |

---

## 🧪 测试指南

### 1. 测试未授权访问

```bash
# 直接访问管理员接口（应返回401）
curl http://localhost:5000/api/admin/pending-users

# 预期响应：
# {"success":false,"message":"未登录或登录已过期，请重新登录"}
```

### 2. 测试正常登录流程

```bash
# 1. 登录获取Session
curl -c cookies.txt -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. 使用Session访问管理员接口
curl -b cookies.txt http://localhost:5000/api/admin/pending-users

# 应该能成功获取数据
```

### 3. 测试Session过期

```bash
# 1. 登录
curl -c cookies.txt -X POST http://localhost:5000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. 等待31分钟（超过30分钟超时时间）

# 3. 再次访问管理员接口（应返回401）
curl -b cookies.txt http://localhost:5000/api/admin/pending-users
```

---

## 🚀 部署建议

### 生产环境配置

1. **使用Redis存储Session**（推荐）

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  session:
    store-type: redis
    timeout: 30m
  redis:
    host: localhost
    port: 6379
```

2. **启用HTTPS**

- Session Cookie应该设置`Secure`标志
- 防止中间人攻击窃取Session ID

3. **配置CORS白名单**

```java
.allowedOrigins("https://yourdomain.com")  // 生产域名
```

4. **增强Session安全**

```java
// 设置Session Cookie属性
session.setMaxInactiveInterval(30 * 60);
// 可以在Filter中添加：
response.setHeader("Set-Cookie", "JSESSIONID=" + session.getId() + 
    "; HttpOnly; Secure; SameSite=Strict");
```

---

## 📝 常见问题

### Q1: 前端报CORS错误

**原因**：CORS配置中未设置`allowCredentials(true)`

**解决**：检查`WebConfig.java`中的CORS配置

### Q2: 401错误后不跳转登录页

**原因**：前端axios未配置`withCredentials: true`

**解决**：检查`api/index.ts`中的axios配置

### Q3: Session频繁过期

**原因**：Session超时时间设置过短

**解决**：调整`session.setMaxInactiveInterval()`参数

### Q4: 重启后端后Session丢失

**原因**：Session存储在内存中

**解决**：使用Redis存储Session（见部署建议）

---

## 📚 相关文档

- [管理员认证指南](./ADMIN_AUTH_GUIDE.md)
- [本地部署指南](./LOCAL_DEPLOYMENT_GUIDE.md)
- [Spring Session文档](https://docs.spring.io/spring-session/reference/)

---

**最后更新**：2025年10月16日
**版本**：v1.0.0

