package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 城市静态数据仓库
 */
@Repository
public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {
    Optional<City> findBySupplierIdAndSupplierCodeAndCityCode(Long supplierId, String supplierCode, String cityCode);
}
