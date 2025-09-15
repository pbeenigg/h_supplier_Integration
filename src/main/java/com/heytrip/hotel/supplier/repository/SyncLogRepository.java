package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 静态同步日志 仓库
 */
@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long>, JpaSpecificationExecutor<SyncLog> {
}
