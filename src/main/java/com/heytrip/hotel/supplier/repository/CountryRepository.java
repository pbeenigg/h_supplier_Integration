package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 国家静态数据仓库
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long>, JpaSpecificationExecutor<Country> {
    Optional<Country> findBySupplierIdAndSupplierCodeAndCountryCode(Long supplierId, String supplierCode, String countryCode);

    List<Country> findAllBySupplierIdAndSupplierCodeAndCountryCodeIn(Long supplierId, String supplierCode, Collection<String> countryCodes);
}
