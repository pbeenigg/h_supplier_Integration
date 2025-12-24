package com.heytrip.hotel.supplier.repository.supplier;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.hotel.supplier.entity.supplier.HotelGiata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * GIATA酒店映射 仓库
 */
@Repository
@DS("aos")
public interface HotelGiataRepository extends JpaRepository<HotelGiata, Long>, JpaSpecificationExecutor<HotelGiata> {
    Optional<HotelGiata> findBySupplierIdAndSupplierCodeAndHotelCodeAndGiataId(Long supplierId, String supplierCode, String hotelCode, String giataId);
}
