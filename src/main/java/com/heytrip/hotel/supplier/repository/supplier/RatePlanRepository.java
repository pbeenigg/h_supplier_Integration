package com.heytrip.hotel.supplier.repository.supplier;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.hotel.supplier.entity.supplier.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 价格计划静态数据仓库
 */
@Repository
@DS("aos")
public interface RatePlanRepository extends JpaRepository<RatePlan, Long>, JpaSpecificationExecutor<RatePlan> {
    Optional<RatePlan> findBySupplierIdAndSupplierCodeAndRatePlanCode(Long supplierId, String supplierCode, String ratePlanCode);
}
