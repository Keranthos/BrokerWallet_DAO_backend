// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title OptimizedNftMinter
 * @dev 优化的NFT铸造合约，支持图片路径存储（而非完整Base64数据）
 * @notice 该合约用于铸造科研贡献纪念NFT，使用后端服务器存储图片
 * @author BrokerWallet Team
 */

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/Counters.sol";

contract OptimizedNftMinter is ERC721, Ownable {
    using Counters for Counters.Counter;
    
    Counters.Counter private _tokenIds;
    
    // NFT元数据结构（优化版，只存储路径和元数据）
    struct NftMetadata {
        string name;              // NFT名称
        string description;       // NFT描述
        string imageMetadata;     // 图片元数据（JSON格式，包含路径、类型、存储方式等）
        string attributes;        // 其他属性（JSON格式）
        uint256 mintTime;         // 铸造时间
        address minter;           // 铸造者地址
        bool exists;              // 是否存在
    }
    
    // Token ID 到 NFT元数据的映射
    mapping(uint256 => NftMetadata) private _tokenMetadata;
    
    // 用户拥有的NFT列表
    mapping(address => uint256[]) private _userNfts;
    
    // 铸造者权限映射
    mapping(address => bool) public minters;
    
    // 铸造费用（默认为0）
    uint256 public mintFee = 0;
    
    // 事件
    event NftMinted(
        uint256 indexed tokenId, 
        address indexed owner, 
        string name,
        uint256 mintTime
    );
    event MinterUpdated(address indexed minter, bool allowed);
    event MintFeeUpdated(uint256 oldFee, uint256 newFee);
    
    constructor() ERC721("Optimized Medal NFT", "OMNFT") {
        // 合约部署者自动获得铸造权限
        minters[msg.sender] = true;
    }
    
    // ==================== 权限管理 ====================
    
    /**
     * @dev 设置铸造者权限
     */
    function setMinter(address account, bool allowed) external onlyOwner {
        minters[account] = allowed;
        emit MinterUpdated(account, allowed);
    }
    
    /**
     * @dev 批量设置铸造者权限
     */
    function batchSetMinter(address[] calldata accounts, bool allowed) external onlyOwner {
        for (uint256 i = 0; i < accounts.length; i++) {
            minters[accounts[i]] = allowed;
            emit MinterUpdated(accounts[i], allowed);
        }
    }
    
    /**
     * @dev 检查铸造权限
     */
    function hasMintPermission(address account) external view returns (bool) {
        return minters[account];
    }
    
    // ==================== 铸造费用管理 ====================
    
    /**
     * @dev 设置铸造费用
     */
    function setMintFee(uint256 newFee) external onlyOwner {
        uint256 oldFee = mintFee;
        mintFee = newFee;
        emit MintFeeUpdated(oldFee, newFee);
    }
    
    /**
     * @dev 提取合约中的费用
     */
    function withdraw() external onlyOwner {
        uint256 balance = address(this).balance;
        require(balance > 0, "No balance to withdraw");
        payable(owner()).transfer(balance);
    }
    
    // ==================== NFT铸造 ====================
    
    /**
     * @dev 铸造NFT（优化版，只存储图片元数据）
     * @param to 接收者地址
     * @param name NFT名称
     * @param description NFT描述
     * @param imageMetadata 图片元数据（JSON格式，包含path, type, storageType, serverUrl等）
     * @param attributes 其他属性（JSON格式）
     */
    function mintNftWithMetadata(
        address to,
        string memory name,
        string memory description,
        string memory imageMetadata,
        string memory attributes
    ) external payable returns (uint256) {
        require(minters[msg.sender], "Not authorized to mint");
        require(msg.value >= mintFee, "Insufficient mint fee");
        require(to != address(0), "Invalid recipient address");
        require(bytes(name).length > 0, "Name cannot be empty");
        require(bytes(imageMetadata).length > 0, "Image metadata cannot be empty");
        
        _tokenIds.increment();
        uint256 newTokenId = _tokenIds.current();
        
        // 铸造NFT
        _mint(to, newTokenId);
        
        // 存储NFT元数据
        _tokenMetadata[newTokenId] = NftMetadata({
            name: name,
            description: description,
            imageMetadata: imageMetadata,
            attributes: attributes,
            mintTime: block.timestamp,
            minter: msg.sender,
            exists: true
        });
        
        // 添加到用户NFT列表
        _userNfts[to].push(newTokenId);
        
        emit NftMinted(newTokenId, to, name, block.timestamp);
        
        return newTokenId;
    }
    
    /**
     * @dev 批量铸造NFT
     */
    function batchMintNftWithMetadata(
        address[] calldata recipients,
        string[] calldata names,
        string[] calldata descriptions,
        string[] calldata imageMetadatas,
        string[] calldata attributesArray
    ) external payable returns (uint256[] memory) {
        require(minters[msg.sender], "Not authorized to mint");
        require(recipients.length > 0, "No recipients");
        require(
            recipients.length == names.length &&
            names.length == descriptions.length &&
            descriptions.length == imageMetadatas.length &&
            imageMetadatas.length == attributesArray.length,
            "Array length mismatch"
        );
        require(msg.value >= mintFee * recipients.length, "Insufficient mint fee");
        
        uint256[] memory tokenIds = new uint256[](recipients.length);
        
        for (uint256 i = 0; i < recipients.length; i++) {
            require(recipients[i] != address(0), "Invalid recipient address");
            require(bytes(names[i]).length > 0, "Name cannot be empty");
            
            _tokenIds.increment();
            uint256 newTokenId = _tokenIds.current();
            
            _mint(recipients[i], newTokenId);
            
            _tokenMetadata[newTokenId] = NftMetadata({
                name: names[i],
                description: descriptions[i],
                imageMetadata: imageMetadatas[i],
                attributes: attributesArray[i],
                mintTime: block.timestamp,
                minter: msg.sender,
                exists: true
            });
            
            _userNfts[recipients[i]].push(newTokenId);
            
            emit NftMinted(newTokenId, recipients[i], names[i], block.timestamp);
            
            tokenIds[i] = newTokenId;
        }
        
        return tokenIds;
    }
    
    // ==================== 查询功能 ====================
    
    /**
     * @dev 获取NFT元数据
     */
    function getNftMetadata(uint256 tokenId) external view returns (
        string memory name,
        string memory description,
        string memory imageMetadata,
        string memory attributes,
        uint256 mintTime,
        address minter,
        address owner
    ) {
        require(_tokenMetadata[tokenId].exists, "NFT does not exist");
        
        NftMetadata memory metadata = _tokenMetadata[tokenId];
        return (
            metadata.name,
            metadata.description,
            metadata.imageMetadata,
            metadata.attributes,
            metadata.mintTime,
            metadata.minter,
            ownerOf(tokenId)
        );
    }
    
    /**
     * @dev 获取用户拥有的所有NFT ID
     */
    function getUserNfts(address user) external view returns (uint256[] memory) {
        return _userNfts[user];
    }
    
    /**
     * @dev 获取用户拥有的NFT数量
     */
    function getUserNftCount(address user) external view returns (uint256) {
        return _userNfts[user].length;
    }
    
    /**
     * @dev 获取总供应量
     */
    function totalSupply() external view returns (uint256) {
        return _tokenIds.current();
    }
    
    /**
     * @dev 批量获取NFT元数据
     */
    function batchGetNftMetadata(uint256[] calldata tokenIds) external view returns (
        string[] memory names,
        string[] memory descriptions,
        string[] memory imageMetadatas,
        string[] memory attributesArray,
        uint256[] memory mintTimes,
        address[] memory nftMinters,
        address[] memory owners
    ) {
        names = new string[](tokenIds.length);
        descriptions = new string[](tokenIds.length);
        imageMetadatas = new string[](tokenIds.length);
        attributesArray = new string[](tokenIds.length);
        mintTimes = new uint256[](tokenIds.length);
        nftMinters = new address[](tokenIds.length);
        owners = new address[](tokenIds.length);
        
        for (uint256 i = 0; i < tokenIds.length; i++) {
            if (_tokenMetadata[tokenIds[i]].exists) {
                NftMetadata memory metadata = _tokenMetadata[tokenIds[i]];
                names[i] = metadata.name;
                descriptions[i] = metadata.description;
                imageMetadatas[i] = metadata.imageMetadata;
                attributesArray[i] = metadata.attributes;
                mintTimes[i] = metadata.mintTime;
                nftMinters[i] = metadata.minter;
                owners[i] = ownerOf(tokenIds[i]);
            }
        }
        
        return (names, descriptions, imageMetadatas, attributesArray, mintTimes, nftMinters, owners);
    }
    
    // ==================== 覆盖函数 ====================
    
    /**
     * @dev 转账时更新用户NFT列表
     */
    function _update(address to, uint256 tokenId, address auth) internal virtual override returns (address) {
        address from = super._update(to, tokenId, auth);
        
        // 如果不是铸造操作，需要更新用户NFT列表
        if (from != address(0) && to != address(0)) {
            // 从发送者列表中移除
            _removeFromUserNfts(from, tokenId);
            // 添加到接收者列表
            _userNfts[to].push(tokenId);
        }
        
        return from;
    }
    
    /**
     * @dev 从用户NFT列表中移除
     */
    function _removeFromUserNfts(address user, uint256 tokenId) private {
        uint256[] storage userNftList = _userNfts[user];
        for (uint256 i = 0; i < userNftList.length; i++) {
            if (userNftList[i] == tokenId) {
                userNftList[i] = userNftList[userNftList.length - 1];
                userNftList.pop();
                break;
            }
        }
    }
}

