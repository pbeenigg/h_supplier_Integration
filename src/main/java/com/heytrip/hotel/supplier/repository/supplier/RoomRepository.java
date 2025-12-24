package com.heytrip.hotel.supplier.repository.supplier;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.heytrip.hotel.supplier.entity.supplier.Room;
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
 * 房型静态数据仓库
 */
@Repository
@DS("aos")
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findBySupplierIdAndSupplierCodeAndRoomCode(Long supplierId, String supplierCode, String roomCode);
    
    List<Room> findBySupplierIdAndSupplierCodeAndHotelCodeAndRoomCodeIn(Long supplierId, String supplierCode, String hotelCode, List<String> roomCodes);

    List<Room> findBySupplierIdAndSupplierCodeAndRoomCodeIn(Long supplierId, String supplierCode, List<String> roomCodes);


    /**
     * 基于自增ID的房型增量查询
     * 查询ID大于指定maxId的房型记录，支持分页
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（房型ID）
     * @param pageable 分页参数
     * @return 分页的房型数据
     */
    @Query("SELECT r FROM Room r WHERE r.supplierId = :supplierId AND r.supplierCode = :supplierCode AND r.id > :maxId ORDER BY r.id ASC")
    Page<Room> findIncrementalRooms(@Param("supplierId") Long supplierId, 
                                   @Param("supplierCode") String supplierCode, 
                                   @Param("maxId") Long maxId, 
                                   Pageable pageable);
    
    /**
     * 基于自增ID和更新时间的房型增量查询
     * 查询ID大于指定maxId且在指定时间范围内更新的房型记录
     * 
     * @param supplierId 供应商ID
     * @param supplierCode 供应商代码
     * @param maxId 上次请求的最大增量编号（房型ID）
     * @param minTime 最小更新时间
     * @param pageable 分页参数
     * @return 分页的房型数据
     */
    @Query("SELECT r FROM Room r WHERE r.supplierId = :supplierId AND r.supplierCode = :supplierCode AND r.id > :maxId AND r.updatedAt >= :minTime ORDER BY r.id ASC")
    Page<Room> findIncrementalRoomsByTime(@Param("supplierId") Long supplierId, 
                                         @Param("supplierCode") String supplierCode, 
                                         @Param("maxId") Long maxId, 
                                         @Param("minTime") LocalDateTime minTime, 
                                         Pageable pageable);
}
