package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 供应商健康检查日志实体类
 * 记录供应商服务健康状态，用于监控供应商服务可用性
 * 
 * @author  Pax
 */
@Entity
@Table(name = "supplier_health_log", indexes = {
    @Index(name = "idx_supplier_id", columnList = "supplierId"),
    @Index(name = "idx_check_type", columnList = "checkType"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_supplier_status_time", columnList = "supplierId, status, createdAt")
})
@Data
public class SupplierHealthLog {
    
    /**
     * 健康检查日志ID，主键自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("健康检查日志ID，主键自增")
    private Long id;
    
    /**
     * 供应商ID，关联supplier_config表
     */
    @Column(name = "supplier_id", nullable = false)
    @Comment("供应商ID，关联supplier_config表")
    private Long supplierId;
    
    /**
     * 检查类型：ping、api_test、full_check等
     */
    @Column(name = "check_type", nullable = false, length = 50)
    @Comment("检查类型：ping、api_test、full_check等")
    private String checkType = "ping";
    
    /**
     * 响应时间，单位毫秒
     */
    @Column(name = "response_time_ms")
    @Comment("响应时间，单位毫秒")
    private Long responseTimeMs;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    @Comment("错误信息")
    private String errorMessage;
    
    /**
     * 检查详情，JSON格式
     */
    @Column(name = "check_details", columnDefinition = "JSON")
    @Comment("检查详情，JSON格式")
    private String checkDetails;
    
    /**
     * 健康状态
     */
    @Column(name = "health_status", length = 20)
    @Comment("健康状态")
    private String healthStatus;
    
    /**
     * 状态消息
     */
    @Column(name = "status_message", length = 500)
    @Comment("状态消息")
    private String statusMessage;
    
    /**
     * 检查时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("检查时间")
    private LocalDateTime createdAt;
    
    // 关联供应商配置
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private SupplierConfig supplierConfig;
    
    // 默认构造函数
    public SupplierHealthLog() {}
    
    // 带参构造函数
    public SupplierHealthLog(Long supplierId, String checkType, HealthStatus healthStatus) {
        this.supplierId = supplierId;
        this.checkType = checkType;
        this.healthStatus = getCodeByEnum(healthStatus);
    }
    
    // 完整构造函数
    public SupplierHealthLog(Long supplierId, String checkType, HealthStatus healthStatus,
                           Long responseTimeMs, String errorMessage, String checkDetails) {
        this.supplierId = supplierId;
        this.checkType = checkType;
        this.healthStatus = getCodeByEnum(healthStatus);
        this.responseTimeMs = responseTimeMs;
        this.errorMessage = errorMessage;
        this.checkDetails = checkDetails;
    }
    

    
    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        UP("UP", "服务正常"),
        DOWN("DOWN", "服务不可用"),
        DEGRADED("DEGRADED", "服务降级"),
        UNKNOWN("UNKNOWN", "状态未知");
        
        private final String code;
        private final String description;
        
        HealthStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }


        public  static HealthStatus fromCode(String code) {
            for (HealthStatus status : HealthStatus.values()) {
                if (status.code.equalsIgnoreCase(code)) {
                    return status;
                }
            }
            return UNKNOWN;
        }


        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }

    public String getCodeByEnum(HealthStatus status) {
        return status.code;
    }
    
    /**
     * 检查类型枚举
     */
    public enum CheckType {
        PING("ping", "基础连通性检查"),
        API_TEST("api_test", "API接口测试"),
        FULL_CHECK("full_check", "完整功能检查"),
        MANUAL("manual", "手动检查"),
        AUTOMATIC("automatic", "自动检查");
        
        private final String code;
        private final String description;
        
        CheckType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
