package com.heytrip.hotel.supplier.repository.supplier;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.hotel.supplier.entity.supplier.DistributionOrdersLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分销商订单日志Repository
 *
 * @author Pax
 * @since 1.0.0
 */
@Repository
@DS("aos")
public interface DistributionOrdersLogRepository extends JpaRepository<DistributionOrdersLog, Long>, JpaSpecificationExecutor<DistributionOrdersLog> {



    /**
     * 根据供应商ID查询最近的订单日志，按创建时间降序排列
     */
    @Query("SELECT acl FROM DistributionOrdersLog acl WHERE acl.supplierId = :supplierId " +
            "ORDER BY acl.createdAt DESC")
    List<DistributionOrdersLog> findRecentCallsBySupplierId(@Param("supplierId") Long supplierId , Pageable pageable);


    /**
     * 根据分销商订单号查询
     */
    List<DistributionOrdersLog> findByDistributionOrdersKey(String distributionOrdersKey);

    /**
     * 根据traceId查询订单日志
     */
    List<DistributionOrdersLog> findByTraceId(String traceId);

    /**
     * 根据供应商预定ID查询
     */
    List<DistributionOrdersLog> findBySupplierBookingKey(String supplierBookingKey);

    DistributionOrdersLog findBySupplierBookingKeyAndBusinessTypeAndIsSuccess(String supplierBookingKey,String businessType,boolean isSuccess);
    DistributionOrdersLog findByDistributionOrdersKeyAndBusinessTypeAndIsSuccess(String distributionOrdersKey,String businessType,boolean isSuccess);

    /**
     * 根据业务类型和时间范围查询
     */
    @Query("SELECT d FROM DistributionOrdersLog d WHERE d.businessType = :businessType " +
           "AND d.createdAt BETWEEN :startTime AND :endTime ORDER BY d.createdAt DESC")
    List<DistributionOrdersLog> findByBusinessTypeAndTimeRange(
            @Param("businessType") String businessType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据业务类型统计订单操作成功率
     */
    @Query("SELECT d.businessType, d.isSuccess, COUNT(d) FROM DistributionOrdersLog d " +
           "WHERE d.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY d.businessType, d.isSuccess")
    List<Object[]> countByBusinessTypeAndSuccess(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);


    Long countByIsSuccessTrue();


    Long countByBusinessType(String businessType);


    long countByBusinessTypeAndIsSuccessTrue(String businessType);
}
