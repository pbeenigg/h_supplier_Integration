package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.ApiCallLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API调用日志数据访问接口
 * 
 * @author  Pax
 */
@Repository
public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {
    
    /**
     * 根据供应商ID查找API调用日志
     */
    List<ApiCallLog> findBySupplierId(Long supplierId);
    
    /**
     * 根据API端点查找调用日志
     */
    List<ApiCallLog> findByApiEndpoint(String apiEndpoint);
    
    /**
     * 根据HTTP方法查找调用日志
     */
    List<ApiCallLog> findByHttpMethod(String httpMethod);
    
    /**
     * 根据响应状态查找调用日志
     */
    List<ApiCallLog> findByResponseStatus(Integer responseStatus);
    
    /**
     * 根据渠道查找调用日志
     */
    List<ApiCallLog> findByChannel(String channel);

    /**
     * 根据应用ID查找调用日志
     */
    List<ApiCallLog> findByAppId(String appId);
    
    /**
     * 根据客户端IP查找调用日志
     */
    List<ApiCallLog> findByClientIp(String clientIp);
    
    /**
     * 查找指定时间段内的API调用日志
     */
    @Query("SELECT acl FROM ApiCallLog acl WHERE acl.createdAt BETWEEN :startTime AND :endTime")
    List<ApiCallLog> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找响应时间超过指定值的调用日志
     */
    @Query("SELECT acl FROM ApiCallLog acl WHERE acl.responseTimeMs > :responseTime")
    List<ApiCallLog> findByResponseTimeGreaterThan(@Param("responseTime") Integer responseTime);
    
    /**
     * 查找有错误信息的调用日志
     */
    @Query("SELECT acl FROM ApiCallLog acl WHERE acl.errorMessage IS NOT NULL")
    List<ApiCallLog> findWithErrors();
    
    /**
     * 统计指定供应商的API调用次数
     */
    @Query("SELECT COUNT(acl) FROM ApiCallLog acl WHERE acl.supplierId = :supplierId")
    Long countCallsBySupplierId(@Param("supplierId") Long supplierId);
    
    /**
     * 统计指定时间段内的成功调用次数
     */
    @Query("SELECT COUNT(acl) FROM ApiCallLog acl WHERE acl.responseStatus BETWEEN 200 AND 299 " +
           "AND acl.createdAt BETWEEN :startTime AND :endTime")
    Long countSuccessfulCallsBetween(@Param("startTime") LocalDateTime startTime, 
                                    @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间段内的失败调用次数
     */
    @Query("SELECT COUNT(acl) FROM ApiCallLog acl WHERE (acl.responseStatus < 200 OR acl.responseStatus >= 400) " +
           "AND acl.createdAt BETWEEN :startTime AND :endTime")
    Long countFailedCallsBetween(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * 计算指定时间段内的平均响应时间
     */
    @Query("SELECT AVG(acl.responseTimeMs) FROM ApiCallLog acl WHERE acl.createdAt BETWEEN :startTime AND :endTime")
    Double calculateAverageResponseTime(@Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找最近的API调用日志（按供应商）
     */
    @Query("SELECT acl FROM ApiCallLog acl WHERE acl.supplierId = :supplierId " +
           "ORDER BY acl.createdAt DESC")
    List<ApiCallLog> findRecentCallsBySupplierId(@Param("supplierId") Long supplierId , Pageable pageable);
    
    /**
     * 根据响应状态统计调用次数
     */
    Long countByResponseStatus(Integer responseStatus);

    /**
     * 计算所有调用的平均响应时间
     */
    @Query("SELECT AVG(acl.responseTimeMs) FROM ApiCallLog acl")
    Double findAverageResponseTime();
    
    /**
     * 统计成功的API调用次数（基于isSuccess字段）
     */
    Long countByIsSuccessTrue();
    
    /**
     * 统计失败的API调用次数（基于isSuccess字段）
     */
    Long countByIsSuccessFalse();
    
    /**
     * 根据业务类型查找调用日志
     */
    List<ApiCallLog> findByBusinessType(String businessType);
    
    /**
     * 根据跟踪ID查找调用日志
     */
    ApiCallLog findByTraceId(String traceId);
    
    /**
     * 根据用户ID查找调用日志
     */
    List<ApiCallLog> findByUserId(String userId);
    
    /**
     * 根据会话ID查找调用日志
     */
    List<ApiCallLog> findBySessionId(String sessionId);
}
