package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.HotelBookable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 可预定酒店仓库
 */
@Repository
public interface HotelBookableRepository extends JpaRepository<HotelBookable, Long>, JpaSpecificationExecutor<HotelBookable> {
    
    /**
     * 按供应商和酒店代码列表删除可预定酒店记录
     * 用于清理本批次要同步的酒店数据
     */
    @Modifying
    @Query("DELETE FROM HotelBookable h WHERE h.supplierId = :supplierId AND h.supplierCode = :supplierCode AND h.hotelCode IN :hotelCodes")
    int deleteBySupplierIdAndSupplierCodeAndHotelCodeIn(
        @Param("supplierId") Long supplierId, 
        @Param("supplierCode") String supplierCode, 
        @Param("hotelCodes") List<String> hotelCodes
    );
    
    /**
     * 按供应商删除所有可预定酒店记录
     * 用于全量同步时的数据清理
     */
    @Modifying
    @Query("DELETE FROM HotelBookable h WHERE h.supplierId = :supplierId AND h.supplierCode = :supplierCode")
    int deleteBySupplierIdAndSupplierCode(
        @Param("supplierId") Long supplierId, 
        @Param("supplierCode") String supplierCode
    );
    
    /**
     * 根据酒店ID查询可预定酒店记录
     */
    List<HotelBookable> findByHotelId(Long hotelId);
    
    /**
     * 根据酒店ID列表查询可预定酒店记录
     */
    List<HotelBookable> findByHotelIdIn(List<Long> hotelIds);
    
    /**
     * 根据供应商和酒店ID查询可预定酒店记录
     */
    HotelBookable findBySupplierIdAndSupplierCodeAndHotelId(Long supplierId, String supplierCode, Long hotelId);
    
    /**
     * 根据酒店ID删除可预定酒店记录
     */
    @Modifying
    @Query("DELETE FROM HotelBookable h WHERE h.hotelId = :hotelId")
    int deleteByHotelId(@Param("hotelId") Long hotelId);
    
    /**
     * 根据酒店ID列表删除可预定酒店记录
     */
    @Modifying
    @Query("DELETE FROM HotelBookable h WHERE h.hotelId IN :hotelIds")
    int deleteByHotelIdIn(@Param("hotelIds") List<Long> hotelIds);
    
    /**
     * 分页查询可预定酒店的酒店代码列表
     * 
     * @param supplierCode 供应商代码
     * @param isBookable 是否可预定
     * @param pageable 分页参数
     * @return 酒店代码分页结果
     */
    @Query("SELECT h.hotelCode FROM HotelBookable h WHERE h.supplierCode = :supplierCode AND h.isBookable = :isBookable ORDER BY h.createdAt DESC")
    Page<String> findHotelCodesBySupplierCodeAndIsBookable(
        @Param("supplierCode") String supplierCode, 
        @Param("isBookable") Boolean isBookable, 
        Pageable pageable
    );
    

}
