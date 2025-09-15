package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 房型静态数据仓库
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findBySupplierIdAndSupplierCodeAndRoomCode(Long supplierId, String supplierCode, String roomCode);
}
