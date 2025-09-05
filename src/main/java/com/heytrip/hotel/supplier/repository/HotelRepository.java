package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 酒店信息数据访问接口
 * 
 * @author  Pax
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {
    
    /**
     * 根据供应商ID和供应商酒店ID查找酒店
     */
    Optional<Hotel> findBySupplierIdAndSupplierHotelId(Long supplierId, String supplierHotelId);
    
    /**
     * 根据城市查找激活的酒店
     */
    @Query("SELECT h FROM Hotel h WHERE h.city = :city AND h.isActive = true")
    List<Hotel> findActiveHotelsByCity(@Param("city") String city);
    
    /**
     * 根据城市查找激活的酒店（带分页限制）
     */
    @Query(value = "SELECT * FROM hotel_info WHERE city = :city AND is_active = true LIMIT :limit", 
           nativeQuery = true)
    List<Hotel> findActiveHotelsByCityWithLimit(@Param("city") String city, @Param("limit") int limit);
    
    /**
     * 根据供应商ID查找所有激活的酒店
     */
    List<Hotel> findBySupplierIdAndIsActiveTrue(Long supplierId);
    
    /**
     * 根据国家查找激活的酒店
     */
    List<Hotel> findByCountryAndIsActiveTrue(String country);
    
    /**
     * 根据星级范围查找酒店
     */
    @Query("SELECT h FROM Hotel h WHERE h.starRating >= :minRating AND h.starRating <= :maxRating AND h.isActive = true")
    List<Hotel> findByStarRatingBetween(@Param("minRating") Double minRating, @Param("maxRating") Double maxRating);
    
    /**
     * 根据酒店名称模糊查询
     */
    @Query("SELECT h FROM Hotel h WHERE h.hotelName LIKE %:name% AND h.isActive = true")
    List<Hotel> findByHotelNameContaining(@Param("name") String name);
    
    /**
     * 统计指定供应商的激活酒店数量
     */
    @Query("SELECT COUNT(h) FROM Hotel h WHERE h.supplierId = :supplierId AND h.isActive = true")
    Long countActiveHotelsBySupplierId(@Param("supplierId") Long supplierId);
    
    /**
     * 根据地理位置范围查找酒店
     */
    @Query("SELECT h FROM Hotel h WHERE h.latitude BETWEEN :minLat AND :maxLat " +
           "AND h.longitude BETWEEN :minLng AND :maxLng AND h.isActive = true")
    List<Hotel> findByLocationBounds(@Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
                                    @Param("minLng") Double minLng, @Param("maxLng") Double maxLng);
    
    /**
     * 根据激活状态统计酒店数量
     */
    Long countByIsActive(Boolean isActive);
}
