package com.heytrip.hotel.supplier.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户管理实体
 * 管理系统登录用户的信息
 *
 * @author Pax
 */
@Entity
@Table(name = "user", indexes = {
        @Index(name = "idx_user_id", columnList = "userId", unique = true),
        @Index(name = "idx_user_name", columnList = "userName", unique = true),
        @Index(name = "idx_user_app_id", columnList = "appId")
})
@Comment("用户管理表")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("用户ID，主键")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Comment("用户名")
    @Column(name = "user_name", length = 50, nullable = false, unique = true)
    private String userName;

    @Comment("密码（密文）")
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Comment("用户昵称")
    @Column(name = "user_nick", length = 100)
    private String userNick;

    @Comment("性别：M=男，F=女，U=未知")
    @Column(name = "sex", length = 1)
    private String sex = "U";

    @Comment("超时时间（小时）：-1不过期，大于-1过期有效")
    @Column(name = "timeout", nullable = false)
    private Integer timeout = -1;

    @Comment("关联的应用ID")
    @Column(name = "app_id", length = 100, nullable = false)
    private String appId;

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

    // 一对一关联App实体（可选，用于查询时的便利性）
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", referencedColumnName = "app_id", insertable = false, updatable = false)
    private App app;

    // 构造函数
    public User() {}

    public User(String userName, String password, String userNick, String sex, Integer timeout, String appId, String createBy) {
        this.userName = userName;
        this.password = password;
        this.userNick = userNick;
        this.sex = sex;
        this.timeout = timeout;
        this.appId = appId;
        this.createBy = createBy;
        this.updateBy = createBy;
    }



    /**
     * 检查用户是否已过期
     */
    public boolean isExpired() {
        if (timeout == -1) {
            return false; // 永不过期
        }
        return createAt.plusHours(timeout).isBefore(LocalDateTime.now());
    }


}
