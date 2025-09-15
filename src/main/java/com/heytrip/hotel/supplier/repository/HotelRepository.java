package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 酒店静态数据仓库
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {
    Optional<Hotel> findBySupplierIdAndSupplierCodeAndHotelCode(Long supplierId, String supplierCode, String hotelCode);
}
