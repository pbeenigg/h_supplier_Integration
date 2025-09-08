package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.SystemConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问接口
 * 提供系统配置的查询和管理功能
 * 
 * @author Pax
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * 根据配置键名查询配置
     * 
     * @param configKey 配置键名
     * @return 系统配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
    
    /**
     * 根据配置键名查询启用的配置
     * 
     * @param configKey 配置键名
     * @return 系统配置
     */
    Optional<SystemConfig> findByConfigKeyAndIsActiveTrue(String configKey);
    
    /**
     * 根据配置类型查询所有配置
     * 
     * @param configType 配置类型
     * @return 系统配置列表
     */
    List<SystemConfig> findByConfigTypeOrderByConfigKey(String configType);
    
    /**
     * 根据配置类型查询启用的配置
     * 
     * @param configType 配置类型
     * @return 系统配置列表
     */
    List<SystemConfig> findByConfigTypeAndIsActiveTrueOrderByConfigKey(String configType);
    
    /**
     * 查询所有启用的配置
     * 
     * @return 系统配置列表
     */
    List<SystemConfig> findByIsActiveTrueOrderByConfigKey();
    
    /**
     * 查询所有加密的配置
     * 
     * @return 系统配置列表
     */
    List<SystemConfig> findByIsEncryptedTrueOrderByConfigKey();
    
    /**
     * 根据配置键名前缀查询配置
     * 
     * @param keyPrefix 配置键名前缀
     * @return 系统配置列表
     */
    List<SystemConfig> findByConfigKeyStartingWithOrderByConfigKey(String keyPrefix);
    
    /**
     * 根据配置键名前缀查询启用的配置
     * 
     * @param keyPrefix 配置键名前缀
     * @return 系统配置列表
     */
    List<SystemConfig> findByConfigKeyStartingWithAndIsActiveTrueOrderByConfigKey(String keyPrefix);
    
    /**
     * 检查配置键名是否存在
     * 
     * @param configKey 配置键名
     * @return 是否存在
     */
    boolean existsByConfigKey(String configKey);
    
    /**
     * 根据配置键名删除配置
     * 
     * @param configKey 配置键名
     * @return 删除的记录数
     */
    int deleteByConfigKey(String configKey);
    
    /**
     * 查询指定配置键名列表的配置
     * 
     * @param configKeys 配置键名列表
     * @return 系统配置列表
     */
    List<SystemConfig> findByConfigKeyInAndIsActiveTrueOrderByConfigKey(List<String> configKeys);
    
    /**
     * 根据创建人查询配置
     * 
     * @param createdBy 创建人
     * @return 系统配置列表
     */
    List<SystemConfig> findByCreatedByOrderByConfigKey(String createdBy);
    
    /**
     * 统计配置总数
     * 
     * @return 配置总数
     */
    @Query("SELECT COUNT(c) FROM SystemConfig c")
    Long countAllConfigs();
    
    /**
     * 统计启用的配置数量
     * 
     * @return 启用的配置数量
     */
    @Query("SELECT COUNT(c) FROM SystemConfig c WHERE c.isActive = true")
    Long countActiveConfigs();
    
    /**
     * 统计加密的配置数量
     * 
     * @return 加密的配置数量
     */
    @Query("SELECT COUNT(c) FROM SystemConfig c WHERE c.isEncrypted = true")
    Long countEncryptedConfigs();
    
    /**
     * 根据配置类型统计数量
     * 
     * @return 配置类型统计：[类型, 数量]
     */
    @Query("SELECT c.configType, COUNT(c) FROM SystemConfig c GROUP BY c.configType")
    List<Object[]> countByConfigType();
    
    /**
     * 查询配置值包含指定文本的配置
     * 
     * @param searchText 搜索文本
     * @return 系统配置列表
     */
    @Query("SELECT c FROM SystemConfig c WHERE c.configValue LIKE %:searchText% " +
           "OR c.description LIKE %:searchText% ORDER BY c.configKey")
    List<SystemConfig> searchByValueOrDescription(@Param("searchText") String searchText);
    
    /**
     * 批量更新配置状态
     * 
     * @param configKeys 配置键名列表
     * @param isActive 是否启用
     * @param updatedBy 更新人
     * @return 更新的记录数
     */
    @Query("UPDATE SystemConfig c SET c.isActive = :isActive, c.updatedBy = :updatedBy " +
           "WHERE c.configKey IN :configKeys")
    int batchUpdateActiveStatus(@Param("configKeys") List<String> configKeys, 
                               @Param("isActive") Boolean isActive, 
                               @Param("updatedBy") String updatedBy);
    
    /**
     * 查询系统初始化相关配置
     * 
     * @return 系统配置列表
     */
    @Query("SELECT c FROM SystemConfig c WHERE c.configKey LIKE 'system.init.%' " +
           "ORDER BY c.configKey")
    List<SystemConfig> findSystemInitConfigs();
    
    /**
     * 查询缓存相关配置
     * 
     * @return 系统配置列表
     */
    @Query("SELECT c FROM SystemConfig c WHERE c.configKey LIKE 'system.cache.%' " +
           "AND c.isActive = true ORDER BY c.configKey")
    List<SystemConfig> findCacheConfigs();
    
    /**
     * 查询安全相关配置
     * 
     * @return 系统配置列表
     */
    @Query("SELECT c FROM SystemConfig c WHERE c.configKey LIKE 'system.security.%' " +
           "AND c.isActive = true ORDER BY c.configKey")
    List<SystemConfig> findSecurityConfigs();
    
    /**
     * 查询通知相关配置
     * 
     * @return 系统配置列表
     */
    @Query("SELECT c FROM SystemConfig c WHERE c.configKey LIKE 'system.notification.%' " +
           "AND c.isActive = true ORDER BY c.configKey")
    List<SystemConfig> findNotificationConfigs();
    
    /**
     * 根据配置类型统计数量（单个类型）
     */
    Long countByConfigType(SystemConfig.ConfigType configType);
    
    /**
     * 根据配置类型查找配置（分页）
     */
    Page<SystemConfig> findByConfigType(SystemConfig.ConfigType configType, Pageable pageable);
    
    /**
     * 统计启用的配置数量
     */
    Long countByIsActiveTrue();
    
    /**
     * 统计禁用的配置数量
     */
    Long countByIsActiveFalse();
    
    /**
     * 统计加密的配置数量
     */
    Long countByIsEncryptedTrue();
    
    /**
     * 统计未加密的配置数量
     */
    Long countByIsEncryptedFalse();
}
