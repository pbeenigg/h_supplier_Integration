package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.SupplierConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 供应商配置数据访问接口
 * 提供供应商配置的查询和管理功能
 * 
 * @author  Pax
 */
@Repository
public interface SupplierConfigRepository extends JpaRepository<SupplierConfig, Long> {
    
    /**
     * 根据供应商名称查找配置
     */
    Optional<SupplierConfig> findBySupplierName(String supplierName);
    
    /**
     * 根据供应商代码查找配置
     */
    Optional<SupplierConfig> findBySupplierCode(String supplierCode);
    
    /**
     * 根据供应商名称和激活状态查找配置
     */
    Optional<SupplierConfig> findBySupplierNameAndIsActive(String supplierName, Boolean isActive);
    
    /**
     * 根据供应商代码和激活状态查找配置
     */
    Optional<SupplierConfig> findBySupplierCodeAndIsActive(String supplierCode, Boolean isActive);
    
    /**
     * 查找所有激活的供应商配置，按优先级排序
     */
    List<SupplierConfig> findByIsActiveTrueOrderByPriority();
    
    /**
     * 查找所有激活的供应商配置
     */
    List<SupplierConfig> findByIsActiveTrue();
    

    /**
     * 根据供应商名称和启用状态查找配置
     */
    Optional<SupplierConfig> findBySupplierNameAndIsActiveTrue(String supplierName);
    
    /**
     * 根据认证类型查找供应商配置
     */
    List<SupplierConfig> findByAuthTypeAndIsActive(String authType, Boolean isActive);
    
    /**
     * 根据优先级范围查找供应商配置
     */
    List<SupplierConfig> findByPriorityBetweenAndIsActiveTrueOrderByPriority(Integer minPriority, Integer maxPriority);
    
    /**
     * 查询超时时间大于指定值的供应商配置
     */
    @Query("SELECT sc FROM SupplierConfig sc WHERE sc.timeoutMs > :timeoutMs AND sc.isActive = true")
    List<SupplierConfig> findByTimeoutMsGreaterThan(@Param("timeoutMs") Long timeoutMs);
    
    /**
     * 查询最大并发请求数小于指定值的供应商配置
     */
    List<SupplierConfig> findByMaxConcurrentRequestsLessThanAndIsActiveTrue(Integer maxConcurrentRequests);
    
    /**
     * 查询每秒请求限制数大于指定值的供应商配置
     */
    List<SupplierConfig> findByRateLimitPerSecondGreaterThanAndIsActiveTrue(Integer rateLimitPerSecond);
    
    /**
     * 根据支持的国家查找供应商配置
     */
    @Query("SELECT sc FROM SupplierConfig sc WHERE sc.supportedCountries LIKE %:country% AND sc.isActive = true")
    List<SupplierConfig> findBySupportedCountriesContaining(@Param("country") String country);
    
    /**
     * 根据支持的城市查找供应商配置
     */
    @Query("SELECT sc FROM SupplierConfig sc WHERE sc.supportedCities LIKE %:city% AND sc.isActive = true")
    List<SupplierConfig> findBySupportedCitiesContaining(@Param("city") String city);
    
    /**
     * 统计激活的供应商数量
     */
    @Query("SELECT COUNT(sc) FROM SupplierConfig sc WHERE sc.isActive = true")
    Long countActiveSuppliers();
    
    /**
     * 统计总供应商数量
     */
    @Query("SELECT COUNT(sc) FROM SupplierConfig sc")
    Long countAllSuppliers();
    
    /**
     * 根据认证类型统计供应商数量
     */
    @Query("SELECT sc.authType, COUNT(sc) FROM SupplierConfig sc WHERE sc.isActive = true GROUP BY sc.authType")
    List<Object[]> countByAuthType();
    
    /**
     * 查询平均响应时间配置
     */
    @Query("SELECT AVG(sc.timeoutMs) FROM SupplierConfig sc WHERE sc.isActive = true")
    Double getAverageTimeoutMs();
    
    /**
     * 查询最高优先级的激活供应商
     */
    Optional<SupplierConfig> findFirstByIsActiveTrueOrderByPriority();
    
    /**
     * 根据创建人查找供应商配置
     */
    List<SupplierConfig> findByCreatedByOrderByCreatedAt(String createdBy);
    
    /**
     * 检查供应商名称是否存在
     */
    boolean existsBySupplierName(String supplierName);
    
    /**
     * 检查供应商代码是否存在
     */
    boolean existsBySupplierCode(String supplierCode);
    
    /**
     * 查询需要健康检查的供应商（激活状态）
     */
    @Query("SELECT sc FROM SupplierConfig sc WHERE sc.isActive = true ORDER BY sc.priority")
    List<SupplierConfig> findSuppliersForHealthCheck();
    
    /**
     * 根据描述关键词搜索供应商
     */
    @Query("SELECT sc FROM SupplierConfig sc WHERE sc.description LIKE %:keyword% " +
           "OR sc.supplierName LIKE %:keyword% ORDER BY sc.priority")
    List<SupplierConfig> searchByKeyword(@Param("keyword") String keyword);
}
