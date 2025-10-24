package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 静态同步日志 仓库
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long>, JpaSpecificationExecutor<SyncLog> {

    /**
     * 根据供应商代码和业务类型，获取最新的一条同步日志记录
     *
     * @param supplierCode 供应商代码
     * @param businessType 业务类型
     * @return 最新的同步日志记录
     */
    Optional<SyncLog> findTopBySupplierCodeAndBusinessTypeOrderByCreatedAtDesc(String supplierCode, String businessType);

    /**
     * 根据供应商代码和业务类型集合，按业务类型分组，每种类型只返回一条最新的记录
     *
     * @param supplierCode 供应商代码
     * @param businessTypes 业务类型集合
     * @return 每种业务类型的最新同步日志记录列表，按创建时间降序
     */
    @Query("SELECT s FROM SyncLog s WHERE s.supplierCode = :supplierCode " +
           "AND s.businessType IN :businessTypes " +
           "AND s.createdAt = (SELECT MAX(s2.createdAt) FROM SyncLog s2 " +
           "WHERE s2.supplierCode = s.supplierCode AND s2.businessType = s.businessType) " +
           "ORDER BY s.createdAt DESC")
    Optional<List<SyncLog>> findLatestBySupplierCodeAndBusinessTypeIn(@Param("supplierCode") String supplierCode,
                                                            @Param("businessTypes") Set<String> businessTypes);

    /**
     * 根据供应商代码和业务类型集合查询，按创建时间降序排列所有同步日志记录
     *
     * @param supplierCode 供应商代码
     * @param businessTypes 业务类型集合
     * @return 同步日志记录列表
     */
    Optional<List<SyncLog>> findBySupplierCodeAndBusinessTypeInOrderByCreatedAtDesc(String supplierCode, Collection<String> businessTypes);


    Long countByIsSuccessTrue();
}
