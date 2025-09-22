package com.brokerwallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import okhttp3.OkHttpClient;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/**
 * 区块链配置类
 * 从根目录的blockchain-config.yml文件读取配置
 */
@Configuration
@PropertySource("classpath:blockchain-config.yml")
@Slf4j
public class BlockchainConfig {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.account-address}")
    private String accountAddress;

    @Value("${blockchain.contracts.medal-contract}")
    private String medalContractAddress;

    @Value("${blockchain.contracts.nft-contract}")
    private String nftContractAddress;

    @Value("${timeout.connect:30}")
    private int connectTimeout;

    @Value("${timeout.read:120}")
    private int readTimeout;

    @Value("${timeout.write:30}")
    private int writeTimeout;

    @Bean
    public Web3j web3j() {
        // 创建OkHttpClient并设置超时时间
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
        
        // 创建HttpService并设置超时时间
        HttpService httpService = new HttpService(rpcUrl, okHttpClient);
        Web3j web3j = Web3j.build(httpService);
        
        // 测试连接
        try {
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("Connected to BrokerChain client: {}", clientVersion);
            
            // 获取当前区块号
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            log.info("Current block number: {}", blockNumber.getBlockNumber());
            
            // 检查账户余额
            try {
                var balance = web3j.ethGetBalance(accountAddress, 
                    org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send().getBalance();
                log.info("Account balance: {} wei", balance);
                log.info("Account balance: {} ETH", balance.divide(java.math.BigInteger.valueOf(10).pow(18)));
            } catch (Exception e) {
                log.warn("Failed to get account balance: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Failed to connect to BrokerChain client: {}", e.getMessage());
        }
        
        return web3j;
    }

    @Bean
    public String medalContractAddress() {
        return medalContractAddress;
    }

    @Bean
    public String nftContractAddress() {
        return nftContractAddress;
    }

    @Bean
    public String accountAddress() {
        return accountAddress;
    }
}
