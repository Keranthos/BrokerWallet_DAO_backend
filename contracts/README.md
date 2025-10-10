# Smart Contracts

This folder contains the smart contracts used by the BrokerWallet backend.

## Current Contracts

### 1. MedalNFT.sol
勋章系统智能合约，负责管理用户勋章的发放、查询和统计。

**主要功能：**
- 为用户分配勋章（金、银、铜）
- 查询用户勋章数量
- 查询全局统计数据（总用户数、最高分、榜首）
- 支持管理员权限管理

**关键方法：**
- `distributeMedal(address user, uint8 medalType, uint256 count)` - 分配勋章
- `getUserMedals(address user)` - 查询用户勋章
- `getGlobalStats()` - 查询全局统计

### 2. OptimizedNftMinter.sol
NFT铸造智能合约，负责用户NFT的铸造、查询和管理。

**主要功能：**
- 铸造NFT（支持默认样式和用户上传图片）
- 查询用户拥有的NFT
- 查询所有NFT（分页支持）
- 支持管理员权限和铸造费用设置

**关键方法：**
- `mintNFT(string memory metadata)` - 铸造NFT
- `getUserNfts(address user)` - 查询用户NFT
- `queryAllNfts()` - 查询所有NFT
- `setMintFee(uint256 newFee)` - 设置铸造费用
- `grantMintPermission(address account)` - 授予铸造权限

## Deployment Information

合约已部署在BrokerChain测试网上。

**合约地址配置：**
请查看 `blockchain-config.yml` 文件获取当前部署的合约地址。

## Contract Interaction

后端通过以下服务与合约交互：
- `BlockchainService.java` - NFT铸造和查询
- `MedalService.java` - 勋章分配和查询

## Development Notes

- 合约使用Solidity 0.8.x版本开发
- 使用Web3j库进行Java交互
- 支持EIP-712签名标准
- Gas优化：动态调整gas limit以适应不同大小的NFT数据

