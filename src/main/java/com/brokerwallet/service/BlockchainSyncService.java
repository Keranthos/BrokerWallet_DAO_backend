package com.brokerwallet.service;

import com.brokerwallet.dto.MedalQueryResult;
import com.brokerwallet.entity.UserAccount;
import com.brokerwallet.repository.UserAccountRepository;

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
     * 每5分钟同步一次区块链数据
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void syncBlockchainData() {
        log.info("开始同步区块链数据...");
        
        try {
            // 同步全局统计
            syncGlobalStats();
            
            // 同步用户勋章数据
            syncUserMedals();
            
            log.info("区块链数据同步完成");
            
        } catch (Exception e) {
            log.error("区块链数据同步失败", e);
        }
    }

    /**
     * 同步全局统计
     */
    private void syncGlobalStats() {
        try {
            log.info("同步全局统计...");
            
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
                log.error("查询全局统计失败: {}", response.getError().getMessage());
                return;
            }
            
            // 解码结果
            List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters()
            );
            
            BigInteger totalGold = (BigInteger) results.get(0).getValue();
            BigInteger totalSilver = (BigInteger) results.get(1).getValue();
            BigInteger totalBronze = (BigInteger) results.get(2).getValue();
            
            log.info("全局统计同步完成 - 总金牌: {}, 总银牌: {}, 总铜牌: {}", 
                    totalGold, totalSilver, totalBronze);
            
        } catch (Exception e) {
            log.error("同步全局统计失败", e);
        }
    }

    /**
     * 同步用户勋章数据
     */
    private void syncUserMedals() {
        try {
            log.info("同步用户勋章数据...");
            
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
                    
                    userAccountRepository.save(user);
                    syncedCount++;
                    
                    log.debug("同步用户 {} 的勋章数据 - 金牌: {}, 银牌: {}, 铜牌: {}", 
                            user.getWalletAddress(), 
                            medalResult.getMedals().getGold(),
                            medalResult.getMedals().getSilver(),
                            medalResult.getMedals().getBronze());
                    
                } catch (Exception e) {
                    log.warn("同步用户 {} 的勋章数据失败: {}", user.getWalletAddress(), e.getMessage());
                }
            }
            
            log.info("用户勋章数据同步完成，共同步 {} 个用户", syncedCount);
            
        } catch (Exception e) {
            log.error("同步用户勋章数据失败", e);
        }
    }

    /**
     * 手动触发同步
     */
    public void manualSync() {
        log.info("手动触发区块链数据同步");
        syncBlockchainData();
    }

    /**
     * 同步指定用户的勋章数据
     */
    public void syncUserMedals(String walletAddress) {
        try {
            log.info("同步用户 {} 的勋章数据", walletAddress);
            
            // 查询链上数据
            MedalQueryResult medalResult = blockchainService.queryUserMedals(walletAddress);
            
            // 更新数据库
            UserAccount user = userAccountRepository.findByWalletAddress(walletAddress);
            if (user != null) {
                user.setGoldMedals(medalResult.getMedals().getGold());
                user.setSilverMedals(medalResult.getMedals().getSilver());
                user.setBronzeMedals(medalResult.getMedals().getBronze());
                user.setTotalMedals(medalResult.getMedals().getTotal());
                
                userAccountRepository.save(user);
                
                log.info("用户 {} 的勋章数据同步完成", walletAddress);
            } else {
                log.warn("未找到钱包地址为 {} 的用户", walletAddress);
            }
            
        } catch (Exception e) {
            log.error("同步用户 {} 的勋章数据失败", walletAddress, e);
        }
    }
}
