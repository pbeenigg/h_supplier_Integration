package com.heytrip.hotel.supplier;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * 酒店供应商对接系统启动类
 * 
 * @author  Pax
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching // 启用缓存
@EnableRetry // 启用重试机制
@EnableAsync // 启用异步处理
@EnableScheduling // 启用定时任务
@Slf4j
public class HotelSupplierApplication {

    /**
     * 设置默认时区为上海时区（CST）
     * 确保在任何环境下都使用正确的时区
     */
    @PostConstruct
    public void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        log.info("应用时区已设置为: {}", TimeZone.getDefault().getID());
        log.info("当前系统时间: {}", new java.util.Date());
    }

    public static void main(String[] args) {
        // JVM启动时设置时区
        System.setProperty("user.timezone", "Asia/Shanghai");

        log.info("Starting HeyTrip Supplier Integration Application...");

        SpringApplication.run(HotelSupplierApplication.class, args);

        log.info("HeyTrip Supplier Integration Application started successfully.");

    }
}
