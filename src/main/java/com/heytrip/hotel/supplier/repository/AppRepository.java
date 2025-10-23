package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 应用管理数据访问接口
 *
 * @author Pax
 */
@Repository
public interface AppRepository extends JpaRepository<App, String>, JpaSpecificationExecutor<App> {

    /**
     * 根据appId查找应用
     */
    Optional<App> findByAppId(String appId);

    /**
     * 检查appId是否存在
     */
    boolean existsByAppId(String appId);


    /**
     * 查找指定创建人的应用
     */
    List<App> findByCreateBy(String createBy);

    /**
     * 根据速率限制范围查找应用
     */
    List<App> findByRateLimitBetween(Integer minRate, Integer maxRate);

    /**
     * 查找即将过期的应用（在指定小时内过期）
     */
    @Query("SELECT a FROM App a WHERE a.timeout > 0 AND a.createAt < :beforeTime AND a.createAt >= :afterTime")
    List<App> findAppsExpiringWithinHours(@Param("beforeTime") LocalDateTime beforeTime, @Param("afterTime") LocalDateTime afterTime);

    /**
     * 根据创建时间范围查找应用
     */
    List<App> findByCreateAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
