package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.SupplierHealthLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 供应商健康检查日志数据访问接口
 * 提供供应商健康状态的查询和统计功能
 * 
 * @author Pax
 */
@Repository
public interface SupplierHealthLogRepository extends JpaRepository<SupplierHealthLog, Long> {
    
    /**
     * 根据供应商ID查询最新的健康检查记录
     * 
     * @param supplierId 供应商ID
     * @return 最新的健康检查记录
     */
    Optional<SupplierHealthLog> findFirstBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    
    /**
     * 根据供应商ID和检查类型查询最新的健康检查记录
     * 
     * @param supplierId 供应商ID
     * @param checkType 检查类型
     * @return 最新的健康检查记录
     */
    Optional<SupplierHealthLog> findFirstBySupplierIdAndCheckTypeOrderByCreatedAtDesc(Long supplierId, String checkType);
    
    /**
     * 根据供应商ID查询指定时间范围内的健康检查记录
     * 
     * @param supplierId 供应商ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 健康检查记录列表
     */
    List<SupplierHealthLog> findBySupplierIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long supplierId, LocalDateTime startTime, LocalDateTime endTime);
    


    /**
     * 查询指定时间范围内的健康检查记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 健康检查记录列表
     */
    List<SupplierHealthLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计供应商在指定时间范围内的健康检查次数
     * 
     * @param supplierId 供应商ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 检查次数
     */
    @Query("SELECT COUNT(h) FROM SupplierHealthLog h WHERE h.supplierId = :supplierId " +
           "AND h.createdAt BETWEEN :startTime AND :endTime")
    Long countBySupplierIdAndTimeRange(@Param("supplierId") Long supplierId, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计供应商在指定时间范围内成功的健康检查次数
     * 
     * @param supplierId 供应商ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成功检查次数
     */
    @Query("SELECT COUNT(h) FROM SupplierHealthLog  h WHERE h.supplierId = :supplierId " +
           "AND h.healthStatus = 'UP' AND h.createdAt BETWEEN :startTime AND :endTime")
    Long countSuccessfulChecksBySupplierIdAndTimeRange(@Param("supplierId") Long supplierId, 
                                                      @Param("startTime") LocalDateTime startTime, 
                                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询供应商的平均响应时间
     * 
     * @param supplierId 供应商ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 平均响应时间（毫秒）
     */
    @Query("SELECT AVG(h.responseTimeMs) FROM SupplierHealthLog h WHERE h.supplierId = :supplierId " +
           "AND h.responseTimeMs IS NOT NULL AND h.createdAt BETWEEN :startTime AND :endTime")
    Double getAverageResponseTimeBySupplierIdAndTimeRange(@Param("supplierId") Long supplierId, 
                                                         @Param("startTime") LocalDateTime startTime, 
                                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询所有供应商的最新健康状态
     * 
     * @return 最新健康检查记录列表
     */
    @Query("SELECT h FROM SupplierHealthLog h WHERE h.id IN " +
           "(SELECT MAX(h2.id) FROM SupplierHealthLog h2 GROUP BY h2.supplierId)")
    List<SupplierHealthLog> findLatestHealthStatusForAllSuppliers();
    
    /**
     * 查询指定检查类型的最新记录
     * 
     * @param checkType 检查类型
     * @return 健康检查记录列表
     */
    @Query("SELECT h FROM SupplierHealthLog h WHERE h.checkType = :checkType " +
           "AND h.id IN (SELECT MAX(h2.id) FROM SupplierHealthLog h2 " +
           "WHERE h2.checkType = :checkType GROUP BY h2.supplierId)")
    List<SupplierHealthLog> findLatestHealthStatusByCheckType(@Param("checkType") String checkType);
    
    /**
     * 删除指定时间之前的健康检查记录（用于数据清理）
     * 
     * @param beforeTime 指定时间
     * @return 删除的记录数
     */
    @Query("DELETE FROM SupplierHealthLog h WHERE h.createdAt < :beforeTime")
    int deleteByCreatedAtBefore(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 查询供应商健康状态统计信息
     * 
     * @param supplierId 供应商ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息：[状态, 数量]
     */
    @Query("SELECT h.healthStatus, COUNT(h) FROM SupplierHealthLog h " +
           "WHERE h.supplierId = :supplierId AND h.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY h.healthStatus")
    List<Object[]> getHealthStatusStatsBySupplierIdAndTimeRange(@Param("supplierId") Long supplierId, 
                                                               @Param("startTime") LocalDateTime startTime, 
                                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询响应时间超过阈值的记录
     * 
     * @param threshold 响应时间阈值（毫秒）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 健康检查记录列表
     */
    List<SupplierHealthLog> findByResponseTimeMsGreaterThanAndCreatedAtBetweenOrderByResponseTimeMsDesc(
            Long threshold, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取最近的健康检查日志（按供应商，分页）
     */
    @Query("SELECT shl FROM SupplierHealthLog shl WHERE shl.supplierId = :supplierId " +
           "ORDER BY shl.createdAt DESC")
    List<SupplierHealthLog> findRecentLogsBySupplierId(@Param("supplierId") Long supplierId, Pageable pageable);
    
    /**
     * 根据供应商ID和健康状态查找日志（分页）
     */
    Page<SupplierHealthLog> findBySupplierIdAndHealthStatus(Long supplierId, SupplierHealthLog.HealthStatus healthStatus, Pageable pageable);
    
    /**
     * 根据供应商ID和检查类型查找日志（分页）
     */
    Page<SupplierHealthLog> findBySupplierIdAndCheckType(Long supplierId, SupplierHealthLog.CheckType checkType, Pageable pageable);
    
    /**
     * 根据供应商ID、健康状态和检查类型查找日志（分页）
     */
    Page<SupplierHealthLog> findBySupplierIdAndHealthStatusAndCheckType(Long supplierId, 
        SupplierHealthLog.HealthStatus healthStatus, SupplierHealthLog.CheckType checkType, Pageable pageable);
    
    /**
     * 根据供应商ID查找日志（分页）
     */
    Page<SupplierHealthLog> findBySupplierId(Long supplierId, Pageable pageable);
}
