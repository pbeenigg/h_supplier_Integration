package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 价格计划静态数据仓库
 */
@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, Long>, JpaSpecificationExecutor<RatePlan> {
    Optional<RatePlan> findBySupplierIdAndSupplierCodeAndRatePlanCode(Long supplierId, String supplierCode, String ratePlanCode);
}
