# 数据库初始化说明

本目录包含数据库初始化所需的文件。

---

## 📁 文件说明

| 文件 | 说明 | 用途 |
|------|------|------|
| `init.sql` | 数据库表结构 | 创建空数据库表 |
| `sample-data.sql` | 示例数据 | 包含测试用的NFT、用户、提交记录等数据 |
| `FINAL_SCHEMA.md` | 数据库结构文档 | 详细的表结构说明 |

---

## 🚀 使用方式

### 方式1：从零开始（空数据库）✅

**适用场景：** 
- 想要一个全新的系统
- 自己测试和开发
- 不需要示例数据

**步骤：**

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE brokerwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# 2. 导入表结构
mysql -u root -p brokerwallet < init.sql

# 3. 验证
mysql -u root -p
USE brokerwallet;
SHOW TABLES;
# 应该看到：proof_files, nft_images, user_accounts

# 4. 清空 uploads 文件夹（可选）
cd ../
rm -rf uploads/*
# 或保留目录结构
mkdir -p uploads/proofs uploads/nft-images uploads/thumbnails
```

**结果：**
- ✅ 数据库表已创建
- ✅ 所有表都是空的
- ✅ uploads 文件夹为空
- ✅ 可以开始自己的测试

---

### 方式2：使用示例数据（推荐用于快速体验）✅

**适用场景：**
- 想要快速体验完整功能
- 查看已有的NFT和数据
- 学习和参考

**步骤：**

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE brokerwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# 2. 导入表结构
mysql -u root -p brokerwallet < init.sql

# 3. 导入示例数据
mysql -u root -p brokerwallet < sample-data.sql

# 4. 验证数据
mysql -u root -p
USE brokerwallet;
SELECT COUNT(*) FROM proof_files;   -- 应该有数据
SELECT COUNT(*) FROM nft_images;    -- 应该有数据
SELECT COUNT(*) FROM user_accounts; -- 应该有数据

# 5. 保留 uploads 文件夹（重要！）
# 不要删除或清空 ../uploads/ 文件夹
# 因为数据库中的文件路径指向这些文件
```

**结果：**
- ✅ 数据库表已创建并包含示例数据
- ✅ uploads 文件夹包含对应的文件
- ✅ 可以立即查看NFT和提交记录
- ✅ 文件路径与数据库记录完全匹配

---

## 📊 示例数据说明

### 包含的数据

| 数据类型 | 数量 | 说明 |
|---------|------|------|
| 用户账户 | 2个 | 测试用户（user_accounts表） |
| 证明文件 | ~15条 | 用户提交的证明材料记录 |
| NFT图片 | ~8个 | 已铸造的NFT记录 |
| 实际文件 | 40个 | uploads文件夹中的实际文件 |

### 示例用户

| 钱包地址 | 显示昵称 | 勋章 | NFT数量 |
|---------|---------|------|---------|
| 0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb | 测试用户1 | 金银铜各若干 | 多个 |
| 0x8c056ccb92c567da3fee27c23d4f2f107f203879 | 管理员 | 若干 | 若干 |

---

## ⚠️ 重要提示

### 关于文件路径

**所有文件路径都是相对路径！**

数据库中存储的路径示例：
```
uploads/proofs/users/6/xxx.pdf
uploads/nft-images/users/6/xxx.jpg
uploads/thumbnails/users/6/thumb_xxx.jpg
```

这些路径是相对于**后端项目根目录**的，因此：

✅ **可以做的：**
- 移动整个 `BrokerWallet-backend` 文件夹到任何位置
- 在不同电脑上运行
- 修改项目文件夹名称

❌ **不能做的：**
- 单独移动 `uploads` 文件夹
- 删除 `uploads` 文件夹中的文件（如果使用示例数据）
- 修改 `uploads` 文件夹内部的目录结构

### 关于服务器URL配置

如果使用示例数据，需要确保 `application.yml` 中的配置正确：

```yaml
brokerwallet:
  server:
    # 本地开发
    url: "http://localhost:5000"
    
    # 或USB调试（使用电脑IP）
    # url: "http://192.168.1.100:5000"
```

这个URL会拼接到文件路径前面，形成完整的访问URL：
```
http://localhost:5000/uploads/nft-images/users/6/xxx.jpg
```

---

## 🔄 切换方式

### 从方式1切换到方式2

```bash
# 导入示例数据
mysql -u root -p brokerwallet < sample-data.sql

# 确保 uploads 文件夹完整
# 如果之前清空了，需要从Git恢复
git restore ../uploads/
```

### 从方式2切换到方式1

```bash
# 清空所有表
mysql -u root -p
USE brokerwallet;
TRUNCATE TABLE proof_files;
TRUNCATE TABLE nft_images;
TRUNCATE TABLE user_accounts;
EXIT;

# 清空 uploads 文件夹（可选）
rm -rf ../uploads/*
mkdir -p ../uploads/proofs ../uploads/nft-images ../uploads/thumbnails
```

---

## 🆘 常见问题

### Q1: 导入数据后，NFT图片无法显示

**原因：** uploads 文件夹中的文件丢失或路径不对

**解决：**
```bash
# 1. 检查文件是否存在
ls -la ../uploads/nft-images/users/

# 2. 如果文件丢失，从Git恢复
cd ..
git restore uploads/

# 3. 重启后端
mvn spring-boot:run
```

### Q2: 数据库导入失败

**原因：** 数据库已存在或字符集不对

**解决：**
```bash
# 1. 删除旧数据库
mysql -u root -p
DROP DATABASE IF EXISTS brokerwallet;
CREATE DATABASE brokerwallet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# 2. 重新导入
mysql -u root -p brokerwallet < init.sql
mysql -u root -p brokerwallet < sample-data.sql
```

### Q3: 移动项目后文件路径错误

**原因：** 使用了绝对路径（不应该发生）

**检查：**
```sql
-- 查看数据库中的路径
SELECT file_path FROM proof_files LIMIT 5;
SELECT image_path FROM nft_images LIMIT 5;

-- 正确的路径应该是：uploads/xxx/xxx
-- 错误的路径可能是：D:/xxx/uploads/xxx 或 /home/xxx/uploads/xxx
```

**解决：**
如果发现绝对路径，需要更新数据库：
```sql
-- 将绝对路径改为相对路径（示例）
UPDATE proof_files SET file_path = REPLACE(file_path, 'D:/brokerwallet/BrokerWallet-backend/', '');
UPDATE nft_images SET image_path = REPLACE(image_path, 'D:/brokerwallet/BrokerWallet-backend/', '');
```

---

## 📚 相关文档

- **本地部署指南：** `../LOCAL_DEPLOYMENT_GUIDE.md`
- **云部署指南：** `../CLOUD_DEPLOYMENT_GUIDE.md`
- **数据库结构：** `FINAL_SCHEMA.md`
- **项目结构：** `../PROJECT_STRUCTURE.md`

---

**最后更新：** 2025年10月10日



