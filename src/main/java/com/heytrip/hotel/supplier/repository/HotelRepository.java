package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 酒店静态数据仓库
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {
    Optional<Hotel> findBySupplierIdAndSupplierCodeAndHotelCode(Long supplierId, String supplierCode, String hotelCode);
    
    List<Hotel> findBySupplierIdAndSupplierCodeAndHotelCodeIn(Long supplierId, String supplierCode, List<String> hotelCodes);
    
    /**
     * 基于自增ID的酒店增量查询
     * 查询ID大于指定maxId的酒店记录，支持分页
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（酒店ID）
     * @param pageable 分页参数
     * @return 分页的酒店数据
     */
    @Query("SELECT h FROM Hotel h WHERE h.supplierId = :supplierId AND h.supplierCode = :supplierCode AND h.id > :maxId ORDER BY h.id ASC")
    Page<Hotel> findIncrementalHotels(@Param("supplierId") Long supplierId, 
                                     @Param("supplierCode") String supplierCode, 
                                     @Param("maxId") Long maxId, 
                                     Pageable pageable);
    
    /**
     * 基于自增ID和更新时间的酒店增量查询
     * 查询ID大于指定maxId且在指定时间范围内更新的酒店记录
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（酒店ID）
     * @param minTime 最小更新时间
     * @param pageable 分页参数
     * @return 分页的酒店数据
     */
    @Query("SELECT h FROM Hotel h WHERE h.supplierId = :supplierId AND h.supplierCode = :supplierCode AND h.id > :maxId AND h.updatedAt >= :minTime ORDER BY h.id ASC")
    Page<Hotel> findIncrementalHotelsByTime(@Param("supplierId") Long supplierId, 
                                           @Param("supplierCode") String supplierCode, 
                                           @Param("maxId") Long maxId, 
                                           @Param("minTime") LocalDateTime minTime, 
                                           Pageable pageable);
}
