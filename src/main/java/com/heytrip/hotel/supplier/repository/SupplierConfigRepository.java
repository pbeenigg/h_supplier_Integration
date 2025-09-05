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
     * 根据供应商名称和激活状态查找配置
     */
    Optional<SupplierConfig> findBySupplierNameAndIsActive(String supplierName, Boolean isActive);
    
    /**
     * 查找所有激活的供应商配置
     */
    List<SupplierConfig> findByIsActiveTrue();
    
    /**
     * 根据认证类型查找供应商配置
     */
    List<SupplierConfig> findByAuthTypeAndIsActive(String authType, Boolean isActive);
    
    /**
     * 查询超时时间大于指定值的供应商配置
     */
    @Query("SELECT sc FROM SupplierConfig sc WHERE sc.timeoutSeconds > :timeoutSeconds AND sc.isActive = true")
    List<SupplierConfig> findByTimeoutSecondsGreaterThan(@Param("timeoutSeconds") Integer timeoutSeconds);
    
    /**
     * 统计激活的供应商数量
     */
    @Query("SELECT COUNT(sc) FROM SupplierConfig sc WHERE sc.isActive = true")
    Long countActiveSuppliers();
}
