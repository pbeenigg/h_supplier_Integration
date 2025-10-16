package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 应用管理实体
 * 管理授权接口请求权限的配置
 *
 * @author Pax
 */
@Entity
@Table(name = "app", indexes = {
        @Index(name = "idx_app_id", columnList = "appId", unique = true)
})
@Comment("应用管理表")
@Data
public class App {

    @Id
    @Comment("应用ID，主键")
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

    @Comment("密钥")
    @Column(name = "secret_key", length = 255, nullable = false)
    private String secretKey;

    @Comment("加密密钥")
    @Column(name = "encryption_key", length = 255, nullable = false)
    private String encryptionKey;

    @Comment("速率限制（请求/小时）")
    @Column(name = "rate_limit", nullable = false)
    private Integer rateLimit = 1000;

    @Comment("超时时间（小时）：-1不过期，大于-1过期有效")
    @Column(name = "timeout", nullable = false)
    private Integer timeout = -1;

    @Comment("更新时间")
    @UpdateTimestamp
    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    @Comment("创建时间")
    @CreationTimestamp
    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @Comment("更新人")
    @Column(name = "update_by", length = 50)
    private String updateBy;

    @Comment("创建人")
    @Column(name = "create_by", length = 50)
    private String createBy;

    // 构造函数
    public App() {}

    public App(String appId, String secretKey, String encryptionKey, Integer rateLimit, Integer timeout, String createBy) {
        this.appId = appId;
        this.secretKey = secretKey;
        this.encryptionKey = encryptionKey;
        this.rateLimit = rateLimit;
        this.timeout = timeout;
        this.createBy = createBy;
        this.updateBy = createBy;
    }



    /**
     * 检查应用是否已过期
     */
    public boolean isExpired() {
        if (timeout == -1) {
            return false; // 永不过期
        }
        return createAt.plusHours(timeout).isBefore(LocalDateTime.now());
    }


}
