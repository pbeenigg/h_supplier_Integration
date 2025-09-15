package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 静态数据同步日志
 * 记录：供应商、业务点（countries/cities/hotels/nationality/giata/all）、
 * 文件名、开始/结束时间、数据量、成功/失败、错误信息、耗时等
 */
@Data
@Entity
@Table(name = "sync_log", indexes = {
        @Index(name = "idx_sync_supplier", columnList = "supplier_id,supplier_code"),
        @Index(name = "idx_sync_business", columnList = "business_type"),
        @Index(name = "idx_sync_created", columnList = "created_at")
})
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    @Comment("供应商ID")
    private Long supplierId;

    @Column(name = "supplier_code", length = 100, nullable = false)
    @Comment("供应商代码")
    private String supplierCode;

    @Column(name = "business_type", length = 50, nullable = false)
    @Comment("业务点：countries/cities/hotels/nationality/giata/all")
    private String businessType;

    @Column(name = "file_name", length = 300)
    @Comment("同步的文件名")
    private String fileName;

    @Column(name = "start_time")
    @Comment("开始时间")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @Comment("结束时间")
    private LocalDateTime endTime;

    @Column(name = "total_count")
    @Comment("解析总行数")
    private Long totalCount;

    @Column(name = "success_count")
    @Comment("成功入库数")
    private Long successCount;

    @Column(name = "skip_count")
    @Comment("跳过行数（主键缺失等）")
    private Long skipCount;

    @Column(name = "error_count")
    @Comment("错误行数")
    private Long errorCount;



    @Column(name = "is_success", nullable = false, columnDefinition = "TINYINT(1)")
    @Comment("是否成功：1-成功，0-失败")
    private Boolean isSuccess = false;



    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("错误信息")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;
}
