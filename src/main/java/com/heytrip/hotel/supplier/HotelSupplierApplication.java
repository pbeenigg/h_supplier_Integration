package com.heytrip.hotel.supplier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    public static void main(String[] args) {

        log.info("Starting HeyTrip Supplier Integration Application...");

        SpringApplication.run(HotelSupplierApplication.class, args);

        log.info("HeyTrip Supplier Integration Application started successfully.");


    }
}
