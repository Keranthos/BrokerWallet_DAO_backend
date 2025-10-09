package com.brokerwallet.service;

import com.brokerwallet.dto.MedalQueryResult;
import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.repository.UserAccountRepository;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Address;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * 区块链数据同步服务
 * 定期同步链上勋章数据到数据库
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainSyncService {

    private final Web3j web3j;
    private final UserAccountRepository userAccountRepository;
    private final BlockchainService blockchainService;

    @Value("${blockchain.contracts.medal-contract}")
    private String medalContractAddress;

    /**
     * 自动同步已禁用，改为按需查询
     * 当用户访问时才查询区块链数据
     */
    // @Scheduled(fixedRate = 300000) // 已禁用自动同步
    public void syncBlockchainData() {
        log.info("Starting blockchain data synchronization...");
        
        try {
            // 同步全局统计
            syncGlobalStats();
            
            // 同步用户勋章数据
            syncUserMedals();
            
            log.info("Blockchain data synchronization completed");
            
        } catch (Exception e) {
            log.error("Blockchain data synchronization failed", e);
        }
    }

    /**
     * 同步全局统计
     */
    private void syncGlobalStats() {
        try {
            log.info("Syncing global statistics...");
            
            // 构建查询函数
            Function function = new Function(
                    "getGlobalStats",
                    Arrays.asList(),
                    Arrays.asList(
                            new TypeReference<Uint256>() {}, // totalGold
                            new TypeReference<Uint256>() {}, // totalSilver
                            new TypeReference<Uint256>() {}  // totalBronze
                    )
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            
            // 调用合约
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, medalContractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();
            
            if (response.hasError()) {
                log.error("Failed to query global statistics: {}", response.getError().getMessage());
                return;
            }
            
            // 解码结果
            List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters()
            );
            
            BigInteger totalGold = (BigInteger) results.get(0).getValue();
            BigInteger totalSilver = (BigInteger) results.get(1).getValue();
            BigInteger totalBronze = (BigInteger) results.get(2).getValue();
            
            log.info("Global statistics sync completed - Total Gold: {}, Total Silver: {}, Total Bronze: {}", 
                    totalGold, totalSilver, totalBronze);
            
        } catch (Exception e) {
            log.error("Global statistics sync failed", e);
        }
    }

    /**
     * 同步用户勋章数据
     */
    private void syncUserMedals() {
        try {
            log.info("Syncing user medal data...");
            
            // 获取所有用户账户
            List<UserAccount> users = userAccountRepository.findAll();
            
            int syncedCount = 0;
            for (UserAccount user : users) {
                try {
                    // 查询用户的链上勋章数据
                    MedalQueryResult medalResult = blockchainService.queryUserMedals(user.getWalletAddress());
                    
                    // 更新数据库中的勋章数据
                    user.setGoldMedals(medalResult.getMedals().getGold());
                    user.setSilverMedals(medalResult.getMedals().getSilver());
                    user.setBronzeMedals(medalResult.getMedals().getBronze());
                    user.setTotalMedals(medalResult.getMedals().getTotal());
                    
                    // 同时更新区块链同步勋章信息
                    user.setBlockchainGoldMedals(medalResult.getMedals().getGold());
                    user.setBlockchainSilverMedals(medalResult.getMedals().getSilver());
                    user.setBlockchainBronzeMedals(medalResult.getMedals().getBronze());
                    user.setBlockchainSyncTime(LocalDateTime.now());
                    
                    userAccountRepository.save(user);
                    syncedCount++;
                    
                    log.debug("Syncing user {} medal data - Gold: {}, Silver: {}, Bronze: {}", 
                            user.getWalletAddress(), 
                            medalResult.getMedals().getGold(),
                            medalResult.getMedals().getSilver(),
                            medalResult.getMedals().getBronze());
                    
                } catch (Exception e) {
                    log.warn("Failed to sync user {} medal data: {}", user.getWalletAddress(), e.getMessage());
                }
            }
            
            log.info("User medal data sync completed, synced {} users", syncedCount);
            
        } catch (Exception e) {
            log.error("User medal data sync failed", e);
        }
    }

    /**
     * 手动触发同步
     */
    public void manualSync() {
        log.info("Manually triggering blockchain data synchronization");
        syncBlockchainData();
    }

    /**
     * 同步指定用户的勋章数据
     */
    public void syncUserMedals(String walletAddress) {
        try {
            log.info("Syncing medal data for user {}", walletAddress);
            
            // 查询链上数据
            MedalQueryResult medalResult = blockchainService.queryUserMedals(walletAddress);
            
            // 更新数据库
            UserAccount user = userAccountRepository.findByWalletAddress(walletAddress).orElse(null);
            if (user != null) {
                user.setGoldMedals(medalResult.getMedals().getGold());
                user.setSilverMedals(medalResult.getMedals().getSilver());
                user.setBronzeMedals(medalResult.getMedals().getBronze());
                user.setTotalMedals(medalResult.getMedals().getTotal());
                
                userAccountRepository.save(user);
                
                log.info("Medal data sync completed for user {}", walletAddress);
            } else {
                log.warn("User not found with wallet address {}", walletAddress);
            }
            
        } catch (Exception e) {
            log.error("Failed to sync medal data for user {}", walletAddress, e);
        }
    }
}
