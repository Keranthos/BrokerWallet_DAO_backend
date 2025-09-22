package com.brokerwallet.service;

import com.brokerwallet.dto.MedalQueryResult;
import com.brokerwallet.dto.DistributeRequest;
import com.brokerwallet.dto.DistributeResponse;
import com.brokerwallet.dto.UnsignedTransactionData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 区块链服务类
 * 提供勋章查询、发放等区块链操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainService {

    private final Web3j web3j;
    
    @Value("${blockchain.contracts.medal-contract}")
    private String medalContractAddress;
    
    @Value("${blockchain.account-address}")
    private String accountAddress;

    @PostConstruct
    public void init() {
        log.info("BlockchainService initialized with medal contract: {}", medalContractAddress);
        log.info("Account address: {}", accountAddress);
    }

    /**
     * 查询用户勋章数量
     */
    public MedalQueryResult queryUserMedals(String address) throws Exception {
        log.info("Querying medals for address: {}", address);
        
        // 构建查询函数
        Function function = new Function(
                "getUserMedals",
                Arrays.asList(new Address(address)),
                Arrays.asList(
                        new TypeReference<Uint256>() {}, // gold
                        new TypeReference<Uint256>() {}, // silver
                        new TypeReference<Uint256>() {}, // bronze
                        new TypeReference<Uint256>() {}  // total
                )
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        // 调用合约
        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, medalContractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            throw new RuntimeException("Contract call failed: " + response.getError().getMessage());
        }
        
        // 解码结果
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        BigInteger gold = (BigInteger) results.get(0).getValue();
        BigInteger silver = (BigInteger) results.get(1).getValue();
        BigInteger bronze = (BigInteger) results.get(2).getValue();
        BigInteger total = (BigInteger) results.get(3).getValue();
        
        return MedalQueryResult.builder()
                .address(address)
                .medals(new MedalQueryResult.Medals(
                        gold.intValue(),
                        silver.intValue(),
                        bronze.intValue(),
                        total.intValue()
                ))
                .build();
    }

    /**
     * 创建未签名交易（供钱包程序签名）
     */
    public UnsignedTransactionData createUnsignedTransaction(DistributeRequest request) throws Exception {
        log.info("Creating unsigned transaction for: {}", request);
        
        // 构建发放勋章函数
        Function function = new Function(
                "distributeMedals",
                Arrays.asList(
                        new Address(request.getTo()),
                        new Uint256(BigInteger.valueOf(request.getGoldQty())),
                        new Uint256(BigInteger.valueOf(request.getSilverQty())),
                        new Uint256(BigInteger.valueOf(request.getBronzeQty()))
                ),
                Collections.emptyList()
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        // 获取nonce
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                accountAddress, DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        
        // 估算gas
        BigInteger gasLimit = BigInteger.valueOf(200000); // 固定gas限制
        BigInteger gasPrice = BigInteger.valueOf(20000000000L); // 20 Gwei
        
        return UnsignedTransactionData.builder()
                .to(medalContractAddress)
                .data(encodedFunction)
                .value("0x0")
                .gas(Numeric.toHexStringWithPrefix(gasLimit))
                .gasPrice(Numeric.toHexStringWithPrefix(gasPrice))
                .nonce(Numeric.toHexStringWithPrefix(nonce))
                .chainId("0x1") // BrokerChain chainId
                .build();
    }

    /**
     * 发放勋章（使用钱包签名）
     */
    public DistributeResponse distributeMedalsWithWalletSigning(DistributeRequest request) throws Exception {
        log.info("Distributing medals with wallet signing: {}", request);
        
        // 创建未签名交易
        UnsignedTransactionData unsignedTx = createUnsignedTransaction(request);
        
        // 构建交易
        Transaction transaction = Transaction.createFunctionCallTransaction(
                accountAddress,
                BigInteger.valueOf(Numeric.decodeQuantity(unsignedTx.getNonce())),
                BigInteger.valueOf(Numeric.decodeQuantity(unsignedTx.getGasPrice())),
                BigInteger.valueOf(Numeric.decodeQuantity(unsignedTx.getGas())),
                unsignedTx.getTo(),
                unsignedTx.getData()
        );
        
        // 发送交易（需要钱包程序签名）
        EthSendTransaction response = web3j.ethSendTransaction(transaction).send();
        
        if (response.hasError()) {
            throw new RuntimeException("Transaction failed: " + response.getError().getMessage());
        }
        
        String transactionHash = response.getTransactionHash();
        log.info("Transaction sent: {}", transactionHash);
        
        return DistributeResponse.builder()
                .success(true)
                .message("勋章发放成功")
                .transactionHash(transactionHash)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 测试合约连接
     */
    public String testContractConnection() throws Exception {
        try {
            // 尝试调用合约的全局统计函数
            Function function = new Function(
                    "getGlobalStats",
                    Collections.emptyList(),
                    Arrays.asList(
                            new TypeReference<Uint256>() {}, // totalGold
                            new TypeReference<Uint256>() {}, // totalSilver
                            new TypeReference<Uint256>() {}  // totalBronze
                    )
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(null, medalContractAddress, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();
            
            if (response.hasError()) {
                return "合约连接失败: " + response.getError().getMessage();
            }
            
            return "合约连接成功，地址: " + medalContractAddress;
            
        } catch (Exception e) {
            return "合约连接失败: " + e.getMessage();
        }
    }
}
