package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.BookingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 预订记录数据访问接口
 * 
 * @author  Pax
 */
@Repository
public interface BookingRecordRepository extends JpaRepository<BookingRecord, Long> {
    
    /**
     * 根据预订参考号查找预订记录
     */
    Optional<BookingRecord> findByBookingReference(String bookingReference);
    
    /**
     * 根据供应商预订ID查找预订记录
     */
    Optional<BookingRecord> findBySupplierBookingId(String supplierBookingId);
    
    /**
     * 根据分销商订单ID查找预订记录
     */
    Optional<BookingRecord> findByDistributorOrderId(String distributorOrderId);
    
    /**
     * 根据供应商ID查找预订记录
     */
    List<BookingRecord> findBySupplierId(Long supplierId);
    
    /**
     * 根据酒店ID查找预订记录
     */
    List<BookingRecord> findByHotelId(Long hotelId);
    
    /**
     * 根据预订状态查找预订记录
     */
    List<BookingRecord> findByBookingStatus(Integer bookingStatus);
    
    /**
     * 根据客人姓名查找预订记录
     */
    List<BookingRecord> findByGuestName(String guestName);
    
    /**
     * 根据入住日期范围查找预订记录
     */
    @Query("SELECT br FROM BookingRecord br WHERE br.checkInDate BETWEEN :startDate AND :endDate")
    List<BookingRecord> findByCheckInDateBetween(@Param("startDate") LocalDate startDate, 
                                                @Param("endDate") LocalDate endDate);
    
    /**
     * 根据渠道查找预订记录
     */
    List<BookingRecord> findByChannel(String channel);
    
    /**
     * 根据渠道公司查找预订记录
     */
    List<BookingRecord> findByChannelCompany(String channelCompany);
    
    /**
     * 查找指定时间段内创建的预订记录
     */
    @Query("SELECT br FROM BookingRecord br WHERE br.createdAt BETWEEN :startTime AND :endTime")
    List<BookingRecord> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定供应商的预订数量
     */
    @Query("SELECT COUNT(br) FROM BookingRecord br WHERE br.supplierId = :supplierId")
    Long countBookingsBySupplierId(@Param("supplierId") Long supplierId);
    
    /**
     * 统计指定状态的预订数量
     */
    @Query("SELECT COUNT(br) FROM BookingRecord br WHERE br.bookingStatus = :status")
    Long countBookingsByStatus(@Param("status") Integer status);
    
    /**
     * 根据预订状态统计数量（Spring Data JPA方法命名约定）
     */
    Long countByBookingStatus(Integer bookingStatus);
    
    /**
     * 查找今日入住的预订记录
     */
    @Query("SELECT br FROM BookingRecord br WHERE br.checkInDate = CURRENT_DATE")
    List<BookingRecord> findTodayCheckIns();
    
    /**
     * 查找明日退房的预订记录
     */
    @Query("SELECT br FROM BookingRecord br WHERE br.checkOutDate = CURRENT_DATE + 1")
    List<BookingRecord> findTomorrowCheckOuts();
}
