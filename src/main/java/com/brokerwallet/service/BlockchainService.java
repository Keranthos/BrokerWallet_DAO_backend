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
import java.util.Optional;

/**
 * 区块链服务类
 * 提供勋章查询、发放等区块链操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainService {

    private final Web3j web3j;
    private final com.brokerwallet.repository.UserAccountRepository userAccountRepository;
    
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
     * 发放勋章（使用钱包签名） - 与contract demo一致
     */
    public DistributeResponse distributeMedalsWithWalletSigning(DistributeRequest request) throws Exception {
        log.info("Distributing medals to: {} (wallet signing)", request.getTo());
        
        // 检查发放权限（与contract demo一致）
        boolean isDistributor = checkDistributorPermission(accountAddress);
        if (!isDistributor) {
            throw new RuntimeException("没有发放权限");
        }
        
        // 创建发放函数调用
        Function distributeFunction = new Function(
                "distributeMedals",
                Arrays.asList(
                        new Address(request.getTo()),
                        new Uint256(BigInteger.valueOf(request.getGoldQty())),
                        new Uint256(BigInteger.valueOf(request.getSilverQty())),
                        new Uint256(BigInteger.valueOf(request.getBronzeQty()))
                ),
                Collections.emptyList()
        );
        
        String encodedFunction = FunctionEncoder.encode(distributeFunction);
        
        // 获取nonce（与contract demo一致）
        BigInteger nonce;
        try {
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    accountAddress, DefaultBlockParameterName.LATEST).send();
            nonce = ethGetTransactionCount.getTransactionCount();
            log.info("Current nonce: {}", nonce);
        } catch (Exception e) {
            log.warn("Failed to get nonce, using default value 0: {}", e.getMessage());
            nonce = BigInteger.ZERO;
        }
        
        // 获取gas price（动态获取，与contract demo一致）
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        log.info("Current gas price: {} wei", gasPrice);
        
        // 创建交易对象（gas limit改为300000，与contract demo一致）
        Transaction transaction = Transaction.createFunctionCallTransaction(
                accountAddress,
                nonce,
                gasPrice,
                BigInteger.valueOf(300000), // gas limit
                medalContractAddress,
                BigInteger.ZERO,
                encodedFunction
        );
        
        log.info("Transaction info:");
        log.info("From: {}", accountAddress);
        log.info("To: {}", medalContractAddress);
        log.info("Nonce: {}", nonce);
        log.info("Gas Price: {}", gasPrice);
        log.info("Gas Limit: 300000");
        log.info("Value: 0");
        log.info("Data: {}", encodedFunction);
        
        // 使用 eth_sendTransaction 发送交易（钱包程序会自动签名）
        EthSendTransaction ethSendTransaction = web3j.ethSendTransaction(transaction).send();
        
        if (ethSendTransaction.hasError()) {
            log.error("Transaction failed: {}", ethSendTransaction.getError().getMessage());
            log.error("Error code: {}", ethSendTransaction.getError().getCode());
            throw new RuntimeException("交易发送失败: " + ethSendTransaction.getError().getMessage());
        }
        
        String txHash = ethSendTransaction.getTransactionHash();
        
        if (txHash != null && !txHash.isEmpty()) {
            log.info("Transaction sent to blockchain, hash: {}", txHash);
            
            // 等待交易确认（与contract demo一致）
            TransactionReceipt receipt = waitForTransactionReceipt(txHash);
            
            return DistributeResponse.builder()
                    .success(true)
                    .message("勋章发放成功")
                    .transactionHash(txHash)
                    .build();
        } else {
            throw new RuntimeException("交易发送失败或未返回交易哈希");
        }
    }
    
    /**
     * 检查发放权限
     */
    private boolean checkDistributorPermission(String address) throws Exception {
        log.info("Checking distributor permission for address: {}", address);
        
        Function distributorsFunction = new Function(
                "distributors",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(distributorsFunction);
        
        EthCall ethCall = web3j.ethCall(
                new Transaction(address, null, null, null, medalContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (ethCall.hasError()) {
            log.error("Error checking distributor permission: {}", ethCall.getError().getMessage());
            return false;
        }
        
        String value = ethCall.getValue();
        log.info("Distributor permission response: {}", value);
        
        if (value == null || value.equals("0x")) {
            log.warn("Contract call returned null or empty response");
            return false;
        }
        
        List<org.web3j.abi.datatypes.Type> decoded = FunctionReturnDecoder.decode(value, distributorsFunction.getOutputParameters());
        
        if (decoded.isEmpty()) {
            log.warn("No decoded values returned from permission check");
            return false;
        }
        
        boolean hasPermission = (Boolean) decoded.get(0).getValue();
        log.info("Has distributor permission: {}", hasPermission);
        
        return hasPermission;
    }
    
    /**
     * 等待交易确认
     */
    private TransactionReceipt waitForTransactionReceipt(String transactionHash) throws Exception {
        log.info("Waiting for transaction receipt: {}", transactionHash);
        
        Optional<TransactionReceipt> receiptOptional = Optional.empty();
        int attempts = 0;
        int maxAttempts = 40; // 最多等待40次，每次2秒 = 80秒
        
        while (attempts < maxAttempts && !receiptOptional.isPresent()) {
            receiptOptional = web3j.ethGetTransactionReceipt(transactionHash).send().getTransactionReceipt();
            
            if (!receiptOptional.isPresent()) {
                Thread.sleep(2000);
                attempts++;
                log.info("Waiting for transaction confirmation... attempt {}/{}", attempts, maxAttempts);
            }
        }
        
        if (receiptOptional.isPresent()) {
            log.info("Transaction confirmed!");
            return receiptOptional.get();
        }
        
        throw new RuntimeException("Transaction confirmation timeout");
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
                return "Failed to query global stats: " + response.getError().getMessage();
            }
            
            // 解码结果
            List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters()
            );
            
            if (results.isEmpty()) {
                return "Global stats query returned empty result";
            }
            
            BigInteger totalGold = (BigInteger) results.get(0).getValue();
            BigInteger totalSilver = (BigInteger) results.get(1).getValue();
            BigInteger totalBronze = (BigInteger) results.get(2).getValue();
            
            return String.format("Global stats - Gold: %d, Silver: %d, Bronze: %d", 
                    totalGold.intValue(), totalSilver.intValue(), totalBronze.intValue());
            
        } catch (Exception e) {
            return "Failed to query global stats: " + e.getMessage();
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
                return "Contract connection failed: " + response.getError().getMessage();
            }
            
            return "Contract connected successfully, address: " + medalContractAddress;
            
        } catch (Exception e) {
            return "Contract connection failed: " + e.getMessage();
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
                        .message("No minting permission")
                        .build();
            }
            
            // 查询铸造费用（关键！）
            BigInteger mintFee = getMintFee();
            log.info("Contract mint fee: {} wei ({} ETH)", mintFee, mintFee.divide(BigInteger.valueOf(1000000000000000000L)));
            
            // 构建铸造函数 - 使用新的OptimizedNftMinter合约
            // 方法名改为 mintNftWithMetadata，第4个参数是imageMetadata（图片元数据）而非完整imageData
            Function mintFunction = new Function(
                    "mintNftWithMetadata",  // 新合约的方法名
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(normalizedAddress),
                            new org.web3j.abi.datatypes.Utf8String(request.getName()),
                            new org.web3j.abi.datatypes.Utf8String(request.getDescription()),
                            new org.web3j.abi.datatypes.Utf8String(request.getImageData()),  // 这里现在是imageMetadata
                            new org.web3j.abi.datatypes.Utf8String(request.getAttributes())
                    ),
                    Arrays.asList(new TypeReference<Uint256>() {})
            );
            
            String encodedFunction = FunctionEncoder.encode(mintFunction);
            log.info("Encoded function data: {}", encodedFunction);
            
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
            
            // ✅ 使用合理的 gas limit（新方案只存储路径JSON，数据量极小）
            // 存储路径JSON只需要约 ~500K gas，设置为 1M 以确保安全
            BigInteger gasLimit = BigInteger.valueOf(1000000L); // 1M gas，足够存储路径JSON
            log.info("=== Gas Limit Setting (Optimized for Path Storage) ===");
            log.info("Set Gas Limit: {} (optimized for JSON metadata)", gasLimit);
            log.info("Estimated gas for ~200 bytes JSON: ~500K, using 1M for safety");
            
            // 创建交易对象 - 使用mintFee作为value（关键修复！）
            Transaction transaction = Transaction.createFunctionCallTransaction(
                    accountAddress,
                    nonce,
                    gasPrice,
                    gasLimit,
                    nftContractAddress,
                    mintFee,  // ✅ 使用查询到的mintFee，而不是0
                    encodedFunction
            );
            
            log.info("Transaction details:");
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
                log.error("Transaction failed: {}", ethSendTransaction.getError().getMessage());
                log.error("Error code: {}", ethSendTransaction.getError().getCode());
                return NftMintResponse.builder()
                        .success(false)
                        .message("NFT铸造失败: " + ethSendTransaction.getError().getMessage())
                        .build();
            }
            
            String txHash = ethSendTransaction.getTransactionHash();
            log.info("Transaction sent successfully, Hash: {}", txHash);
            
            // 等待交易确认（与contract demo一致）
            TransactionReceipt receipt = waitForTransactionReceipt(txHash);
            log.info("Transaction confirmed!");
            
            // 解析返回的Token ID - 从交易收据中获取
            String tokenId = parseTokenIdFromReceipt(receipt);
            if (tokenId == null) {
                // 如果无法解析，使用总供应量作为tokenId
                try {
                    BigInteger totalSupply = getTotalSupply();
                    tokenId = totalSupply.toString();
                    log.info("Using total supply as token ID: {}", tokenId);
                } catch (Exception e) {
                    log.warn("Failed to get total supply, using tx hash as token ID");
                    tokenId = txHash;
                }
            }
            
            return NftMintResponse.builder()
                    .success(true)
                    .message("NFT minted successfully")
                    .transactionHash(txHash)
                    .tokenId(tokenId)
                    .imageHash("blockchain://" + tokenId)
                    .metadataHash("blockchain://" + tokenId)
                    .build();
                    
        } catch (Exception e) {
            log.error("=== NFT Minting Exception Details ===");
            log.error("Exception type: {}", e.getClass().getSimpleName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Stack trace:", e);
            return NftMintResponse.builder()
                    .success(false)
                    .message("NFT minting failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 查询用户拥有的NFT
     */
    public NftQueryResult queryUserNfts(String address, int page, int size) throws Exception {
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
                            .mintTime(metadata.getMintTime())
                            .minter(metadata.getMinter())
                            .build();
                    
                    nftList.add(nftInfo);
                    
                } catch (Exception e) {
                    log.warn("Failed to get NFT data for token ID {}: {}", tokenId.getValue(), e.getMessage());
                    // 添加默认信息
                    NftQueryResult.NftInfo nftInfo = NftQueryResult.NftInfo.builder()
                            .tokenId(tokenId.getValue().toString())
                            .ownerAddress(address)
                            .name("NFT #" + tokenId.getValue())
                            .description("Data loading failed")
                            .imageUrl("")
                            .attributes("")  // 空字符串
                            .build();
                    nftList.add(nftInfo);
                }
            }
            
            // 应用分页逻辑
            int totalCount = nftList.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalCount);
            
            List<NftQueryResult.NftInfo> paginatedNfts;
            if (startIndex >= totalCount) {
                paginatedNfts = new java.util.ArrayList<>();
            } else {
                paginatedNfts = nftList.subList(startIndex, endIndex);
            }
            
            log.info("NFT pagination result: Total={}, Page={}, PageSize={}, ReturnCount={}", 
                    totalCount, page, size, paginatedNfts.size());
            
            return NftQueryResult.builder()
                    .address(address)
                    .nfts(paginatedNfts)
                    .totalCount(totalCount)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to query user NFTs: {}", e.getMessage(), e);
            return NftQueryResult.builder()
                    .address(address)
                    .nfts(new java.util.ArrayList<>())
                    .totalCount(0)
                    .build();
        }
    }

    /**
     * 查询所有NFT
     * 通过totalSupply()获取总数量，然后遍历所有Token ID
     */
    public NftQueryResult queryAllNfts(int page, int size) throws Exception {
        log.info("Querying all NFTs: page={}, size={}", page, size);
        
        try {
            // 获取总供应量
            BigInteger totalSupply = getTotalSupply();
            log.info("Total NFT supply: {}", totalSupply);
            
            if (totalSupply.equals(BigInteger.ZERO)) {
                return NftQueryResult.builder()
                        .address("all")
                        .nfts(new java.util.ArrayList<>())
                        .totalCount(0)
                        .build();
            }
            
            List<NftQueryResult.NftInfo> nftList = new java.util.ArrayList<>();
            
            // 遍历所有Token ID
            for (BigInteger tokenId = BigInteger.ONE; tokenId.compareTo(totalSupply) <= 0; tokenId = tokenId.add(BigInteger.ONE)) {
                try {
                    // 直接获取NFT数据（如果不存在会抛出异常）
                    NftQueryResult.NftMetadata metadata = getNftData(tokenId);
                    
                    // 获取NFT所有者
                    String ownerAddress = getNftOwner(tokenId);
                    
                    // 查询所有者的花名
                    String ownerDisplayName = getOwnerDisplayName(ownerAddress);
                    log.info("NFT #{} 持有者: {} ({})", tokenId, ownerAddress, ownerDisplayName);
                    
                    NftQueryResult.NftInfo nftInfo = NftQueryResult.NftInfo.builder()
                            .tokenId(tokenId.toString())
                            .ownerAddress(ownerAddress)
                            .ownerDisplayName(ownerDisplayName)
                            .name(metadata.getName())
                            .description(metadata.getDescription())
                            .imageUrl(metadata.getImage())
                            .attributes(metadata.getAttributes())
                            .mintTime(metadata.getMintTime())
                            .minter(metadata.getMinter())
                            .build();
                    
                    log.info("NftInfo构建完成: tokenId={}, ownerDisplayName={}", nftInfo.getTokenId(), nftInfo.getOwnerDisplayName());
                    nftList.add(nftInfo);
                } catch (Exception e) {
                    log.error("Failed to get NFT data for token ID {}: {}", tokenId, e.getMessage(), e);
                    // 跳过这个NFT，继续处理下一个
                }
            }
            
            // 应用分页逻辑
            int totalCount = nftList.size();
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalCount);
            
            List<NftQueryResult.NftInfo> paginatedNfts;
            if (startIndex >= totalCount) {
                paginatedNfts = new java.util.ArrayList<>();
            } else {
                paginatedNfts = nftList.subList(startIndex, endIndex);
            }
            
            log.info("All NFTs pagination result: Total={}, Page={}, PageSize={}, ReturnCount={}", 
                    totalCount, page, size, paginatedNfts.size());
            
            return NftQueryResult.builder()
                    .address("all")
                    .nfts(paginatedNfts)
                    .totalCount(totalCount)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to query all NFTs: {}", e.getMessage(), e);
            return NftQueryResult.builder()
                    .address("all")
                    .nfts(new java.util.ArrayList<>())
                    .totalCount(0)
                    .build();
        }
    }

    /**
     * 获取总供应量（公开方法，供其他服务调用）
     */
    public BigInteger getTotalSupply() throws Exception {
        Function function = new Function(
                "totalSupply",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Uint256>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            throw new RuntimeException("Failed to get total supply: " + response.getError().getMessage());
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        if (results.isEmpty()) {
            return BigInteger.ZERO;
        }
        
        return (BigInteger) results.get(0).getValue();
    }
    
    /**
     * 根据钱包地址查询用户花名
     */
    private String getOwnerDisplayName(String ownerAddress) {
        try {
            if (ownerAddress == null || ownerAddress.trim().isEmpty()) {
                log.debug("持有者地址为空，返回匿名用户");
                return "匿名用户";
            }
            
            // 标准化地址（去掉0x前缀，统一为小写）
            String normalizedAddress = ownerAddress.toLowerCase();
            if (normalizedAddress.startsWith("0x")) {
                normalizedAddress = normalizedAddress.substring(2);
            }
            log.debug("查询持有者花名: 原始地址={}, 标准化地址={}", ownerAddress, normalizedAddress);
            
            // 查询用户
            Optional<com.brokerwallet.entity.UserAccount> userOptional = 
                    userAccountRepository.findByWalletAddress(normalizedAddress);
            
            if (userOptional.isPresent()) {
                String displayName = userOptional.get().getDisplayName();
                log.debug("找到用户: 地址={}, 花名={}", normalizedAddress, displayName);
                return (displayName != null && !displayName.trim().isEmpty()) 
                        ? displayName : "匿名用户";
            }
            
            log.debug("未找到用户: 地址={}", normalizedAddress);
            return "匿名用户";
        } catch (Exception e) {
            log.warn("Failed to get display name for address {}: {}", ownerAddress, e.getMessage());
            return "匿名用户";
        }
    }
    
    /**
     * 获取NFT所有者
     */
    private String getNftOwner(BigInteger tokenId) throws Exception {
        Function function = new Function(
                "ownerOf",
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(new TypeReference<Address>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            return "unknown";
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        if (results.isEmpty()) {
            return "unknown";
        }
        
        return results.get(0).getValue().toString();
    }

    /**
     * 获取NFT元数据（适配新合约OptimizedNftMinter）
     */
    private NftQueryResult.NftMetadata getNftData(BigInteger tokenId) throws Exception {
        // 新合约使用 getNftMetadata 方法，返回更多字段
        Function function = new Function(
                "getNftMetadata",  // 新合约的方法名
                Arrays.asList(new Uint256(tokenId)),
                Arrays.asList(
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // name
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // description
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // imageMetadata（新：图片元数据JSON）
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}, // attributes
                        new TypeReference<Uint256>() {},                             // mintTime（新）
                        new TypeReference<Address>() {},                             // minter（新）
                        new TypeReference<Address>() {}                              // owner（新）
                )
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            throw new RuntimeException("Failed to get NFT data: " + response.getError().getMessage());
        }
        
        String responseValue = response.getValue();
        log.info("NFT metadata raw response for token {}: {}", tokenId, responseValue);
        log.info("Response value length: {}", responseValue != null ? responseValue.length() : "null");
        
        // 验证响应数据长度
        if (responseValue == null || responseValue.length() < 64) {
            log.error("Invalid response data for token {}: too short (length: {})", 
                    tokenId, responseValue != null ? responseValue.length() : "null");
            throw new RuntimeException("Invalid NFT metadata response: data too short");
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                responseValue, function.getOutputParameters()
        );
        
        log.info("Decoded {} fields from NFT metadata", results.size());
        
        // 新合约返回7个字段
        if (results.size() >= 7) {
            String name = (String) results.get(0).getValue();
            String description = (String) results.get(1).getValue();
            String imageMetadata = (String) results.get(2).getValue();  // 图片元数据（JSON格式）
            String attributes = (String) results.get(3).getValue();
            BigInteger mintTime = (BigInteger) results.get(4).getValue();  // 铸造时间（Unix时间戳）
            String minter = (String) results.get(5).getValue();            // 铸造者地址
            // String owner = (String) results.get(6).getValue();             // 可选：所有者
            
            log.info("NFT metadata parsing result:");
            log.info("Name: {}", name);
            log.info("Description: {}", description);
            log.info("Image metadata: {}", imageMetadata);
            log.info("Attributes: {}", attributes);
            log.info("Mint time: {}", mintTime);
            log.info("Minter: {}", minter);
            
            // 验证NFT数据质量
            if (name == null || name.trim().isEmpty()) {
                log.warn("NFT name is empty, using default");
                name = "NFT #" + tokenId;
            }
            
            if (description == null || description.trim().isEmpty()) {
                log.warn("NFT description is empty, using default");
                description = "No description available";
            }
            
            // 构建图片URL（从图片元数据中提取）
            String imageUrl = buildImageUrl(imageMetadata);
            log.info("Final image URL: {}", imageUrl);
            
            // 格式化铸造时间（Unix时间戳转为可读格式）
            String formattedMintTime = formatUnixTimestamp(mintTime);
            
            return NftQueryResult.NftMetadata.builder()
                    .name(name)
                    .description(description)
                    .image(imageUrl)
                    .attributes(attributes)  // 直接传递JSON字符串，不解析
                    .mintTime(formattedMintTime)
                    .minter(minter)
                    .build();
        }
        
        throw new RuntimeException("NFT data format error");
    }

    /**
     * 构建图片URL（处理JSON元数据格式）
     */
    private String buildImageUrl(String imageMetadata) {
        if (imageMetadata == null || imageMetadata.isEmpty()) {
            log.warn("Image metadata is null or empty");
            return "";
        }
        
        try {
            log.info("Processing image metadata: {}", imageMetadata.substring(0, Math.min(200, imageMetadata.length())));
            
            // 尝试解析JSON格式的图片元数据
            if (imageMetadata.startsWith("{") && imageMetadata.contains("storageType")) {
                try {
                    // 使用Jackson解析JSON
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode metadata = mapper.readTree(imageMetadata);
                    
                    String storageType = metadata.has("storageType") ? metadata.get("storageType").asText() : "";
                    
                    if ("backend-server".equals(storageType)) {
                        // ✅ 新格式：后端服务器存储的图片
                        String path = metadata.has("path") ? metadata.get("path").asText() : "";
                        String imageType = metadata.has("type") ? metadata.get("type").asText() : "image/jpeg";
                        
                        if (!path.isEmpty()) {
                            // ✅ 返回图片URL，由前端使用Glide加载
                            String serverUrl = metadata.has("serverUrl") ? metadata.get("serverUrl").asText() : "http://localhost:5000";
                            // 确保path以/开头，避免URL拼接错误
                            if (!path.startsWith("/")) {
                                path = "/" + path;
                            }
                            String imageUrl = serverUrl + path;
                            log.info("✅ Returning image URL for frontend: {}", imageUrl);
                            return imageUrl;
                        } else {
                            log.warn("Image path is empty in metadata");
                            return "";
                        }
                    } else {
                        log.warn("Unknown storage type: {}", storageType);
                        return "";
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse image metadata as JSON: {}", e.getMessage());
                    // 继续尝试其他格式
                }
            }
            
            // 兼容旧格式：Base64 data URL
            if (imageMetadata.startsWith("data:")) {
                log.info("Image metadata is a data URL (legacy format)");
                if (isValidBase64Data(imageMetadata)) {
                    log.info("Data URL is valid, using as-is");
                    return imageMetadata;
                } else {
                    log.warn("Invalid base64 data format in data URL");
                    return "";
                }
            }
            
            // 兼容旧格式：纯Base64数据
            if (isValidBase64String(imageMetadata)) {
                log.info("Image metadata is base64 string (legacy format), adding SVG prefix");
                return "data:image/svg+xml;base64," + imageMetadata;
            }
            
            log.warn("Image metadata format not recognized");
            return "";
            
        } catch (Exception e) {
            log.error("Error processing image metadata: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * 验证Base64数据是否有效
     */
    private boolean isValidBase64Data(String dataUrl) {
        try {
            if (!dataUrl.contains(",")) {
                return false;
            }
            String[] parts = dataUrl.split(",", 2);
            if (parts.length != 2) {
                return false;
            }
            String base64Part = parts[1];
            // 尝试解码Base64
            java.util.Base64.getDecoder().decode(base64Part);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证Base64字符串是否有效
     */
    private boolean isValidBase64String(String base64String) {
        try {
            java.util.Base64.getDecoder().decode(base64String);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 格式化Unix时间戳为可读格式（支持毫秒级和秒级）
     */
    private String formatUnixTimestamp(BigInteger timestamp) {
        try {
            if (timestamp == null || timestamp.equals(BigInteger.ZERO)) {
                return "";
            }
            
            long value = timestamp.longValue();
            java.time.Instant instant;
            
            // 判断是毫秒级（13位）还是秒级（10位）时间戳
            if (value > 10000000000L) {
                // 毫秒级时间戳（13位数字）
                instant = java.time.Instant.ofEpochMilli(value);
                log.debug("Formatting millisecond timestamp: {}", value);
            } else {
                // 秒级时间戳（10位数字）
                instant = java.time.Instant.ofEpochSecond(value);
                log.debug("Formatting second timestamp: {}", value);
            }
            
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(
                    instant, java.time.ZoneId.systemDefault());
            // 格式化为 "2025-10-07 18:48:12"
            String formatted = dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.debug("Formatted timestamp {} to {}", value, formatted);
            return formatted;
        } catch (Exception e) {
            log.warn("Failed to format timestamp: {}", e.getMessage());
            return timestamp.toString();
        }
    }

    /**
     * 查询NFT铸造费用
     */
    public BigInteger getMintFee() throws Exception {
        log.info("Querying NFT mint fee");
        
        Function function = new Function(
                "mintFee",
                Collections.emptyList(),
                Arrays.asList(new TypeReference<Uint256>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            log.error("Failed to query mint fee: {}", response.getError().getMessage());
            return BigInteger.ZERO;
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        if (results.isEmpty()) {
            log.warn("No mint fee returned, assuming 0");
            return BigInteger.ZERO;
        }
        
        BigInteger mintFee = (BigInteger) results.get(0).getValue();
        log.info("NFT mint fee: {} wei", mintFee);
        return mintFee;
    }
    
    /**
     * 检查铸造权限
     */
    public boolean hasMintPermission(String address) throws Exception {
        log.info("=== Checking NFT Minting Permission ===");
        log.info("Checking address: {}", address);
        log.info("NFT contract address: {}", nftContractAddress);
        
        Function function = new Function(
                "hasMintPermission",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<org.web3j.abi.datatypes.Bool>() {})
        );
        
        String encodedFunction = FunctionEncoder.encode(function);
        log.info("Encoded function: {}", encodedFunction);
        
        EthCall response = web3j.ethCall(
                new Transaction(accountAddress, null, null, null, nftContractAddress, BigInteger.ZERO, encodedFunction),
                DefaultBlockParameterName.LATEST
        ).send();
        
        if (response.hasError()) {
            log.error("Error checking mint permission: {}", response.getError().getMessage());
            return false;
        }
        
        String value = response.getValue();
        log.info("Permission check response: {}", value);
        
        if (value == null || value.equals("0x")) {
            log.warn("Contract call returned null or empty response");
            return false;
        }
        
        List<org.web3j.abi.datatypes.Type> results = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters()
        );
        
        if (results.isEmpty()) {
            log.warn("No decoded values returned from permission check");
            return false;
        }
        
        boolean hasPermission = (Boolean) results.get(0).getValue();
        log.info("=== Mint Permission Result: {} ===", hasPermission ? "✅ HAS PERMISSION" : "❌ NO PERMISSION");
        
        return hasPermission;
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
    
    /**
     * 从交易收据中解析Token ID（与contract demo一致）
     */
    private String parseTokenIdFromReceipt(TransactionReceipt receipt) {
        try {
            if (receipt == null || receipt.getLogs() == null) {
                log.warn("Transaction receipt is empty or has no logs");
                return null;
            }
            
            // 查找 NftMinted 事件
            for (org.web3j.protocol.core.methods.response.Log logEntry : receipt.getLogs()) {
                // NftMinted 事件的签名: NftMinted(uint256 indexed tokenId, address indexed owner, string name)
                if (logEntry.getTopics() != null && logEntry.getTopics().size() >= 2) {
                    // 第二个主题是 tokenId (indexed)
                    String tokenIdHex = logEntry.getTopics().get(1);
                    if (tokenIdHex != null && tokenIdHex.length() >= 3) {
                        try {
                            BigInteger tokenId = new BigInteger(tokenIdHex.substring(2), 16);
                            log.info("Parsed Token ID from receipt: {}", tokenId);
                            return tokenId.toString();
                        } catch (NumberFormatException e) {
                            log.warn("Failed to parse Token ID: {}", tokenIdHex);
                        }
                    }
                }
            }
            
            log.warn("NftMinted event not found in transaction receipt");
            return null;
            
        } catch (Exception e) {
            log.error("Failed to parse transaction receipt: {}", e.getMessage());
            return null;
        }
    }
    
    public String getAccountAddress() {
        return accountAddress;
    }
}
