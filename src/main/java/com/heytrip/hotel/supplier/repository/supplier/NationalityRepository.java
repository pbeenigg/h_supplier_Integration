package com.heytrip.hotel.supplier.repository.supplier;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.hotel.supplier.constant.CacheNames;
import com.heytrip.hotel.supplier.entity.supplier.Nationality;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 国籍静态数据仓库
 */
@Repository
@DS("aos")
public interface NationalityRepository extends JpaRepository<Nationality, Long>, JpaSpecificationExecutor<Nationality> {
    Optional<Nationality> findBySupplierIdAndSupplierCodeAndNationalityCode(Long supplierId, String supplierCode, String nationalityCode);

    @Cacheable(cacheNames = CacheNames.NATIONALITY, key = "'ONE:'+ #supplierId + ':' + #supplierCode + ':' + #isoCode")
    Optional<Nationality> findBySupplierIdAndSupplierCodeAndIsoCode(Long supplierId, String supplierCode, String isoCode);
}
