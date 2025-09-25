package com.brokerwallet.service;

import com.brokerwallet.dto.MedalQueryResult;
import com.brokerwallet.dto.DistributeRequest;
import com.brokerwallet.dto.DistributeResponse;
import com.brokerwallet.dto.UnsignedTransactionData;
import com.brokerwallet.dto.NftMintRequest;
import com.brokerwallet.dto.NftMintResponse;
import com.brokerwallet.dto.NftQueryResult;

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
import org.web3j.abi.datatypes.Utf8String;
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
    
    @Value("${blockchain.contracts.nft-contract}")
    private String nftContractAddress;
    
    @Value("${blockchain.account-address}")
    private String accountAddress;

    @PostConstruct
    public void init() {
        log.info("BlockchainService initialized with medal contract: {}", medalContractAddress);
        log.info("NFT contract: {}", nftContractAddress);
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
                new Transaction(address, null, null, null, medalContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            throw new RuntimeException("Contract call failed: " + response.getError().getMessage());
        }
        
        // 解码结果
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        // 检查结果是否为空
        if (results.isEmpty()) {
            log.warn("Contract call returned empty results for address: {}", address);
            return MedalQueryResult.builder()
                    .address(address)
                    .medals(new MedalQueryResult.Medals(0, 0, 0, 0))
                    .build();
        }
        
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
                Numeric.decodeQuantity(unsignedTx.getNonce()),
                Numeric.decodeQuantity(unsignedTx.getGasPrice()),
                Numeric.decodeQuantity(unsignedTx.getGas()),
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
     * 查询全局统计
     */
    public String queryGlobalStats() throws Exception {
        try {
            // 调用合约的全局统计函数
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
                    new Transaction(accountAddress, null, null, null, medalContractAddress, BigInteger.ZERO, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();
            
            if (response.hasError()) {
                return "查询全局统计失败: " + response.getError().getMessage();
            }
            
            // 解码结果
            List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters()
            );
            
            if (results.isEmpty()) {
                return "全局统计查询返回空结果";
            }
            
            BigInteger totalGold = (BigInteger) results.get(0).getValue();
            BigInteger totalSilver = (BigInteger) results.get(1).getValue();
            BigInteger totalBronze = (BigInteger) results.get(2).getValue();
            
            return String.format("全局统计 - 金牌: %d, 银牌: %d, 铜牌: %d", 
                    totalGold.intValue(), totalSilver.intValue(), totalBronze.intValue());
            
        } catch (Exception e) {
            return "查询全局统计失败: " + e.getMessage();
        }
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
                    new Transaction(accountAddress, null, null, null, medalContractAddress, BigInteger.ZERO, encodedFunction),
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

    /**
     * 铸造NFT
     */
    public NftMintResponse mintNft(NftMintRequest request) throws Exception {
        log.info("Minting NFT for address: {}", request.getOwnerAddress());
        
        try {
            // 使用contract项目的方式处理地址
            String ownerAddress = request.getOwnerAddress();
            if (!ownerAddress.startsWith("0x")) {
                ownerAddress = "0x" + ownerAddress;
            }
            String normalizedAddress = ownerAddress.toLowerCase();
            log.info("Normalized address: {}", normalizedAddress);
            
            // 检查铸造权限
            boolean hasPermission = hasMintPermission(accountAddress);
            if (!hasPermission) {
                return NftMintResponse.builder()
                        .success(false)
                        .message("没有铸造权限")
                        .build();
            }
            
            // 构建铸造函数 - 使用contract项目的方式
            Function mintFunction = new Function(
                    "mintNftWithData",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(normalizedAddress),
                            new org.web3j.abi.datatypes.Utf8String(request.getName()),
                            new org.web3j.abi.datatypes.Utf8String(request.getDescription()),
                            new org.web3j.abi.datatypes.Utf8String(request.getImageData()),
                            new org.web3j.abi.datatypes.Utf8String(request.getAttributes())
                    ),
                    Arrays.asList(new TypeReference<Uint256>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(mintFunction);
            log.info("编码后的函数数据: {}", encodedFunction);
            
            // 获取nonce - 使用contract项目的方式
            BigInteger nonce;
            try {
                var nonceResponse = web3j.ethGetTransactionCount(accountAddress, DefaultBlockParameterName.LATEST).send();
                log.info("Nonce response raw: {}", nonceResponse.getResult());
                
                if (nonceResponse.hasError()) {
                    log.error("Failed to get nonce: {}", nonceResponse.getError().getMessage());
                    nonce = BigInteger.ZERO;
                } else {
                    nonce = nonceResponse.getTransactionCount();
                    log.info("Nonce: {}", nonce);
                }
            } catch (Exception e) {
                log.error("Exception while getting nonce: {}", e.getMessage());
                nonce = BigInteger.ZERO;
            }
            
            // 获取gas price - 使用contract项目的方式
            BigInteger gasPrice;
            try {
                var gasPriceResponse = web3j.ethGasPrice().send();
                log.info("Gas price response raw: {}", gasPriceResponse.getResult());
                
                if (gasPriceResponse.hasError()) {
                    log.error("Failed to get gas price: {}", gasPriceResponse.getError().getMessage());
                    gasPrice = BigInteger.valueOf(1000000000L); // 1 Gwei as fallback
                } else {
                    gasPrice = gasPriceResponse.getGasPrice();
                    log.info("Gas Price: {}", gasPrice);
                }
            } catch (Exception e) {
                log.error("Exception while getting gas price: {}", e.getMessage());
                gasPrice = BigInteger.valueOf(1000000000L); // 1 Gwei as fallback
            }
            
            // 直接设置非常大的 gas limit，确保足够
            BigInteger gasLimit = BigInteger.valueOf(8000000); // 8M gas，非常保守的设置
            log.info("=== CRITICAL: GAS LIMIT 强制设置 ===");
            log.info("设置的 Gas Limit: {}", gasLimit);
            log.info("Gas Limit 数值: {}", gasLimit.longValue());
            log.info("Gas Limit 字符串: {}", gasLimit.toString());
            
            // 创建交易对象 - 使用contract项目的方式
            Transaction transaction = Transaction.createFunctionCallTransaction(
                    accountAddress,
                    nonce,
                    gasPrice,
                    gasLimit,
                    nftContractAddress,
                    BigInteger.ZERO,
                    encodedFunction
            );
            
            log.info("交易详情:");
            log.info("From: {}", transaction.getFrom());
            log.info("To: {}", transaction.getTo());
            log.info("Nonce: {}", transaction.getNonce());
            log.info("Gas Price: {}", transaction.getGasPrice());
            log.info("Gas Limit: {}", gasLimit);
            log.info("Value: {}", transaction.getValue());
            log.info("Data: {}", transaction.getData());
            
            // 发送交易
            EthSendTransaction ethSendTransaction = web3j.ethSendTransaction(transaction).send();
            
            if (ethSendTransaction.hasError()) {
                log.error("交易失败: {}", ethSendTransaction.getError().getMessage());
                log.error("错误代码: {}", ethSendTransaction.getError().getCode());
                return NftMintResponse.builder()
                        .success(false)
                        .message("NFT铸造失败: " + ethSendTransaction.getError().getMessage())
                        .build();
            }
            
            String txHash = ethSendTransaction.getTransactionHash();
            log.info("交易发送成功，Hash: {}", txHash);
            
            return NftMintResponse.builder()
                    .success(true)
                    .message("NFT铸造成功")
                    .transactionHash(txHash)
                    .imageHash("blockchain://" + txHash)
                    .metadataHash("blockchain://" + txHash)
                    .build();
                    
        } catch (Exception e) {
            log.error("=== NFT铸造异常详情 ===");
            log.error("异常类型: {}", e.getClass().getSimpleName());
            log.error("异常消息: {}", e.getMessage());
            log.error("堆栈跟踪:", e);
            return NftMintResponse.builder()
                    .success(false)
                    .message("NFT铸造失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 查询用户拥有的NFT
     */
    public NftQueryResult queryUserNfts(String address) throws Exception {
        log.info("Querying NFTs for address: {}", address);
        
        try {
            // 构建查询函数
            Function function = new Function(
                    "getUserNfts",
                    Arrays.asList(new Address(address)),
                    Arrays.asList(new TypeReference<org.web3j.abi.datatypes.DynamicArray<Uint256>>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(function);
            
            // 调用合约
            EthCall response = web3j.ethCall(
                    new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                    DefaultBlockParameterName.LATEST
            ).send();
            
            if (response.hasError()) {
                throw new RuntimeException("Contract call failed: " + response.getError().getMessage());
            }
            
            // 解码结果
            List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters()
            );
            
            if (results.isEmpty()) {
                return NftQueryResult.builder()
                        .address(address)
                        .nfts(new java.util.ArrayList<>())
                        .totalCount(0)
                        .build();
            }
            
            // 解析Token IDs
            @SuppressWarnings("unchecked")
            org.web3j.abi.datatypes.DynamicArray<Uint256> tokenIds = 
                    (org.web3j.abi.datatypes.DynamicArray<Uint256>) results.get(0);
            
            List<NftQueryResult.NftInfo> nftList = new java.util.ArrayList<>();
            
            for (Uint256 tokenId : tokenIds.getValue()) {
                try {
                    // 获取NFT数据
                    NftQueryResult.NftMetadata metadata = getNftData(tokenId.getValue());
                    
                    NftQueryResult.NftInfo nftInfo = NftQueryResult.NftInfo.builder()
                            .tokenId(tokenId.getValue().toString())
                            .ownerAddress(address)
                            .name(metadata.getName())
                            .description(metadata.getDescription())
                            .imageUrl(metadata.getImage())
                            .attributes(metadata.getAttributes())
                            .build();
                    
                    nftList.add(nftInfo);
                    
                } catch (Exception e) {
                    log.warn("Failed to get NFT data for token ID {}: {}", tokenId.getValue(), e.getMessage());
                    // 添加默认信息
                    NftQueryResult.NftInfo nftInfo = NftQueryResult.NftInfo.builder()
                            .tokenId(tokenId.getValue().toString())
                            .ownerAddress(address)
                            .name("NFT #" + tokenId.getValue())
                            .description("数据加载失败")
                            .imageUrl("")
                            .attributes(new java.util.ArrayList<>())
                            .build();
                    nftList.add(nftInfo);
                }
            }
            
            return NftQueryResult.builder()
                    .address(address)
                    .nfts(nftList)
                    .totalCount(nftList.size())
                    .build();
                    
        } catch (Exception e) {
            log.error("查询用户NFT失败: {}", e.getMessage(), e);
            return NftQueryResult.builder()
                    .address(address)
                    .nfts(new java.util.ArrayList<>())
                    .totalCount(0)
                    .build();
        }
    }

    /**
     * 获取NFT数据
     */
    private NftQueryResult.NftMetadata getNftData(BigInteger tokenId) throws Exception {
        Function function = new Function(
                "getNftData",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // name
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // description
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // imageData
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}  // attributes
                )
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            throw new RuntimeException("获取NFT数据失败: " + response.getError().getMessage());
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        if (results.size() >= 4) {
            String name = (String) results.get(0).getValue();
            String description = (String) results.get(1).getValue();
            String imageData = (String) results.get(2).getValue();
            String attributes = (String) results.get(3).getValue();
            
            // 构建图片URL
            String imageUrl = buildImageUrl(imageData);
            
            // 解析属性
            List<Object> attributesList = parseAttributes(attributes);
            
            return NftQueryResult.NftMetadata.builder()
                    .name(name)
                    .description(description)
                    .image(imageUrl)
                    .attributes(attributesList)
                    .build();
        }
        
        throw new RuntimeException("NFT数据格式错误");
    }

    /**
     * 构建图片URL
     */
    private String buildImageUrl(String imageData) {
        if (imageData == null || imageData.isEmpty()) {
            return "";
        }
        
        if (imageData.startsWith("data:")) {
            return imageData;
        }
        
        return "data:image/svg+xml;base64," + imageData;
    }

    /**
     * 解析属性JSON
     */
    private List<Object> parseAttributes(String attributes) {
        List<Object> attributesList = new java.util.ArrayList<>();
        try {
            if (attributes != null && !attributes.isEmpty() && !attributes.equals("[]")) {
                // 简单的JSON解析，实际项目中可以使用Jackson
                // 这里简化处理
            }
        } catch (Exception e) {
            log.warn("解析属性JSON失败: {}", e.getMessage());
        }
        return attributesList;
    }

    /**
     * 检查铸造权限
     */
    private boolean hasMintPermission(String address) throws Exception {
        Function function = new Function(
                "hasMintPermission",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            return false;
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        return !results.isEmpty() && (Boolean) results.get(0).getValue();
    }
    
    /**
     * 标准化以太坊地址格式
     */
    private String normalizeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }
        
        // 移除前后空格并转换为小写
        return address.trim().toLowerCase();
    }
}
