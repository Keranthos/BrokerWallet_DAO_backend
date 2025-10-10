// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title MedalSystem
 * @dev 科研社区勋章发放系统智能合约
 * @notice 管理金银铜三种勋章的发放、查询和统计功能
 * @author BlockEmulator Team
 */

import "@openzeppelin/contracts/access/Ownable.sol";

contract MedalSystem is Ownable {
    // 三种勋章类型
    uint256 public constant GOLD = 1;
    uint256 public constant SILVER = 2;
    uint256 public constant BRONZE = 3;
    
    // 发放者权限映射
    mapping(address => bool) public distributors;
    
    // 用户勋章数量映射：address => (medalType => count)
    mapping(address => mapping(uint256 => uint256)) public userMedals;
    
    // 用户总勋章数量
    mapping(address => uint256) public totalUserMedals;
    
    // 全局勋章统计
    uint256 public totalGoldMedals;
    uint256 public totalSilverMedals;
    uint256 public totalBronzeMedals;
    
    // 事件
    event DistributorUpdated(address indexed account, bool allowed);
    event MedalsDistributed(address indexed to, uint256 gold, uint256 silver, uint256 bronze);
    
    constructor() Ownable(msg.sender) {
        // 合约部署者自动获得发放权限
        distributors[msg.sender] = true;
    }
    
    // 设置发放者权限
    function setDistributor(address account, bool allowed) external onlyOwner {
        distributors[account] = allowed;
        emit DistributorUpdated(account, allowed);
    }
    
    // 检查发放权限
    function hasDistributorPermission(address account) external view returns (bool) {
        return distributors[account];
    }
    
    // 发放勋章
    function distributeMedals(
        address to,
        uint256 goldQty,
        uint256 silverQty,
        uint256 bronzeQty
    ) external {
        require(distributors[msg.sender], "Not authorized to distribute");
        require(to != address(0), "Invalid recipient address");
        
        if (goldQty > 0) {
            userMedals[to][GOLD] += goldQty;
            totalGoldMedals += goldQty;
            totalUserMedals[to] += goldQty;
        }
        
        if (silverQty > 0) {
            userMedals[to][SILVER] += silverQty;
            totalSilverMedals += silverQty;
            totalUserMedals[to] += silverQty;
        }
        
        if (bronzeQty > 0) {
            userMedals[to][BRONZE] += bronzeQty;
            totalBronzeMedals += bronzeQty;
            totalUserMedals[to] += bronzeQty;
        }
        
        emit MedalsDistributed(to, goldQty, silverQty, bronzeQty);
    }
    
    // 查询用户勋章数量
    function getUserMedals(address user) external view returns (
        uint256 gold,
        uint256 silver,
        uint256 bronze,
        uint256 total
    ) {
        gold = userMedals[user][GOLD];
        silver = userMedals[user][SILVER];
        bronze = userMedals[user][BRONZE];
        total = totalUserMedals[user];
    }
    
    // 查询全局统计
    function getGlobalStats() external view returns (
        uint256 totalGold,
        uint256 totalSilver,
        uint256 totalBronze
    ) {
        totalGold = totalGoldMedals;
        totalSilver = totalSilverMedals;
        totalBronze = totalBronzeMedals;
    }
}
