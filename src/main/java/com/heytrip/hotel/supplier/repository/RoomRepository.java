package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 房间信息数据访问接口
 * 
 * @author  Pax
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    /**
     * 根据酒店ID和供应商房间ID查找房间
     */
    Optional<Room> findByHotelIdAndSupplierRoomId(Long hotelId, String supplierRoomId);
    
    /**
     * 根据酒店ID查找所有激活的房间
     */
    List<Room> findByHotelIdAndIsActiveTrue(Long hotelId);
    
    /**
     * 根据房间类型查找激活的房间
     */
    List<Room> findByRoomTypeAndIsActiveTrue(String roomType);
    
    /**
     * 根据床型查找激活的房间
     */
    List<Room> findByBedTypeAndIsActiveTrue(String bedType);
    
    /**
     * 根据最大入住人数查找房间
     */
    @Query("SELECT r FROM Room r WHERE r.maxOccupancy >= :occupancy AND r.isActive = true")
    List<Room> findByMaxOccupancyGreaterThanEqual(@Param("occupancy") Integer occupancy);
    
    /**
     * 根据房间名称模糊查询
     */
    @Query("SELECT r FROM Room r WHERE r.roomName LIKE %:name% AND r.isActive = true")
    List<Room> findByRoomNameContaining(@Param("name") String name);
    
    /**
     * 查找有窗房间
     */
    List<Room> findByHasWindowTrueAndIsActiveTrue();
    
    /**
     * 查找禁烟房间
     */
    List<Room> findBySmokingAllowedFalseAndIsActiveTrue();
    
    /**
     * 统计指定酒店的激活房间数量
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.hotelId = :hotelId AND r.isActive = true")
    Long countActiveRoomsByHotelId(@Param("hotelId") Long hotelId);
    
    /**
     * 根据房间大小范围查找房间
     */
    @Query("SELECT r FROM Room r WHERE r.roomSize BETWEEN :minSize AND :maxSize AND r.isActive = true")
    List<Room> findByRoomSizeBetween(@Param("minSize") Integer minSize, @Param("maxSize") Integer maxSize);
}
