package com.heytrip.hotel.supplier.repository;

import com.heytrip.hotel.supplier.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户管理数据访问接口
 *
 * @author Pax
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUserName(String userName);

    /**
     * 根据appId查找用户
     */
    Optional<User> findByAppId(String appId);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUserName(String userName);

    /**
     * 检查appId是否已被用户使用
     */
    boolean existsByAppId(String appId);


    /**
     * 根据用户昵称模糊查找
     */
    List<User> findByUserNickContaining(String userNick);

    /**
     * 根据性别查找用户
     */
    List<User> findBySex(String sex);

    /**
     * 根据创建人查找用户
     */
    List<User> findByCreateBy(String createBy);


    /**
     * 根据创建时间范围查找用户
     */
    List<User> findByCreateAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找用户及其关联的App信息
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.app WHERE u.userName = :userName")
    Optional<User> findByUserNameWithApp(@Param("userName") String userName);

    /**
     * 查找用户及其关联的App信息
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.app WHERE u.userId = :userId")
    Optional<User> findByUserIdWithApp(@Param("userId") Long userId);
}
