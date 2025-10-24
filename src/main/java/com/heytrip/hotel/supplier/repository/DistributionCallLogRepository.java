package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.DistributionCallLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分销商API调用日志Repository
 *
 * @author Pax
 * @since 1.0.0
 */
@Repository
public interface DistributionCallLogRepository extends JpaRepository<DistributionCallLog, Long>, JpaSpecificationExecutor<DistributionCallLog> {



    /**
     * 根据供应商ID查询最近的调用日志，按创建时间降序排列
     */
    @Query("SELECT acl FROM DistributionCallLog acl WHERE acl.supplierId = :supplierId " +
            "ORDER BY acl.createdAt DESC")
    List<DistributionCallLog> findRecentCallsBySupplierId(@Param("supplierId") Long supplierId , Pageable pageable);

    /**
     * 根据traceId查询日志
     */
    List<DistributionCallLog> findByTraceId(String traceId);

    /**
     * 根据供应商ID和时间范围查询
     */
    @Query("SELECT d FROM DistributionCallLog d WHERE d.supplierId = :supplierId " +
           "AND d.createdAt BETWEEN :startTime AND :endTime ORDER BY d.createdAt DESC")
    List<DistributionCallLog> findBySupplierIdAndTimeRange(
            @Param("supplierId") Long supplierId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据业务类型查询
     */
    List<DistributionCallLog> findByBusinessTypeAndCreatedAtBetween(
            String businessType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计成功失败数量
     */
    @Query("SELECT d.isSuccess, COUNT(d) FROM DistributionCallLog d " +
           "WHERE d.createdAt BETWEEN :startTime AND :endTime GROUP BY d.isSuccess")
    List<Object[]> countBySuccessStatus(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);


    Long countByIsSuccessTrue();
}
