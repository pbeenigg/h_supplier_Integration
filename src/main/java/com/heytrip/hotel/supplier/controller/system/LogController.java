package com.heytrip.hotel.supplier.controller.system;

import com.heytrip.hotel.supplier.dto.R;
import com.heytrip.hotel.supplier.entity.ApiCallLog;
import com.heytrip.hotel.supplier.entity.DistributionCallLog;
import com.heytrip.hotel.supplier.entity.DistributionOrdersLog;
import com.heytrip.hotel.supplier.repository.ApiCallLogRepository;
import com.heytrip.hotel.supplier.repository.DistributionCallLogRepository;
import com.heytrip.hotel.supplier.repository.DistributionOrdersLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志管理Controller
 * 提供API调用日志、分销商调用日志、分销商订单日志的查询功能
 *
 * @author Pax
 */
@RestController
@RequestMapping("/logs")
@Validated
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    // 分页常量
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private ApiCallLogRepository apiCallLogRepository;

    @Autowired
    private DistributionCallLogRepository distributionCallLogRepository;

    @Autowired
    private DistributionOrdersLogRepository distributionOrdersLogRepository;

    /**
     * 获取API调用日志
     *
     * @param appId 应用ID
     * @param supplierId 供应商ID
     * @param traceId 链路追踪ID
     * @param isSuccess 是否成功
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码（从0开始）
     * @param size 页大小
     * @return API调用日志分页数据
     */
    @GetMapping("/supplier/call")
    public R<Map<String, Object>> getSupplierCallLogs(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) Boolean isSuccess,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Getting supplier call logs with params: appId={}, supplierId={}, traceId={}, isSuccess={}, startTime={}, endTime={}, page={}, size={}",
                appId, supplierId, traceId, isSuccess, startTime, endTime, page, size);

        try {
            // 验证分页参数
            size = Math.min(size, MAX_PAGE_SIZE);
            if (size <= 0) size = DEFAULT_PAGE_SIZE;
            if (page < 0) page = 0;

            // 创建分页对象，按创建时间降序
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // 构建查询条件
            Specification<ApiCallLog> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (appId != null && !appId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("appId"), appId));
                }
                if (supplierId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
                }
                if (traceId != null && !traceId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("traceId"), traceId));
                }
                if (isSuccess != null) {
                    predicates.add(criteriaBuilder.equal(root.get("isSuccess"), isSuccess));
                }
                if (startTime != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
                }
                if (endTime != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            // 执行查询
            Page<ApiCallLog> pageResult = apiCallLogRepository.findAll(spec, pageable);

            // 构建返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("content", pageResult.getContent());
            response.put("totalElements", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("currentPage", pageResult.getNumber());
            response.put("pageSize", pageResult.getSize());
            response.put("hasNext", pageResult.hasNext());
            response.put("hasPrevious", pageResult.hasPrevious());

            return R.ok("获取API调用日志成功", response);

        } catch (Exception e) {
            logger.error("Failed to get supplier call logs", e);
            return R.fail("获取API调用日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取分销商调用日志
     *
     * @param appId 应用ID
     * @param supplierId 供应商ID
     * @param traceId 链路追踪ID
     * @param businessType 业务类型
     * @param isSuccess 是否成功
     * @param hotelKey 酒店标识
     * @param distributionOrdersKey 分销商订单Key
     * @param checkInKey 入住日期Key
     * @param checkOutKey 退房日期Key
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码（从0开始）
     * @param size 页大小
     * @return 分销商调用日志分页数据
     */
    @GetMapping("/distribution/call")
    public R<Map<String, Object>> getDistributionCallLogs(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Boolean isSuccess,
            @RequestParam(required = false) String hotelKey,
            @RequestParam(required = false) String distributionOrdersKey,
            @RequestParam(required = false) String checkInKey,
            @RequestParam(required = false) String checkOutKey,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Getting distribution call logs with params: appId={}, supplierId={}, traceId={}, businessType={}, isSuccess={}, page={}, size={}",
                appId, supplierId, traceId, businessType, isSuccess, page, size);

        try {
            // 验证分页参数
            size = Math.min(size, MAX_PAGE_SIZE);
            if (size <= 0) size = DEFAULT_PAGE_SIZE;
            if (page < 0) page = 0;

            // 创建分页对象，按创建时间降序
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // 构建查询条件
            Specification<DistributionCallLog> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (appId != null && !appId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("appId"), appId));
                }
                if (supplierId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
                }
                if (traceId != null && !traceId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("traceId"), traceId));
                }
                if (businessType != null && !businessType.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("businessType"), businessType));
                }
                if (isSuccess != null) {
                    predicates.add(criteriaBuilder.equal(root.get("isSuccess"), isSuccess));
                }
                if (hotelKey != null && !hotelKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("hotelKey"), hotelKey));
                }
                if (distributionOrdersKey != null && !distributionOrdersKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("distributionOrdersKey"), distributionOrdersKey));
                }
                if (checkInKey != null && !checkInKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("checkInKey"), checkInKey));
                }
                if (checkOutKey != null && !checkOutKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("checkOutKey"), checkOutKey));
                }
                if (startTime != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
                }
                if (endTime != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            // 执行查询
            Page<DistributionCallLog> pageResult = distributionCallLogRepository.findAll(spec, pageable);

            // 构建返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("content", pageResult.getContent());
            response.put("totalElements", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("currentPage", pageResult.getNumber());
            response.put("pageSize", pageResult.getSize());
            response.put("hasNext", pageResult.hasNext());
            response.put("hasPrevious", pageResult.hasPrevious());

            return R.ok("获取分销商调用日志成功", response);

        } catch (Exception e) {
            logger.error("Failed to get distribution call logs", e);
            return R.fail("获取分销商调用日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取分销商订单日志
     *
     * @param appId 应用ID
     * @param supplierId 供应商ID
     * @param traceId 链路追踪ID
     * @param businessType 业务类型
     * @param isSuccess 是否成功
     * @param hotelKey 酒店标识
     * @param distributionOrdersKey 分销商订单Key
     * @param checkInKey 入住日期Key
     * @param checkOutKey 退房日期Key
     * @param supplierBookingKey 供应商预订Key
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码（从0开始）
     * @param size 页大小
     * @return 分销商订单日志分页数据
     */
    @GetMapping("/distribution/orders")
    public R<Map<String, Object>> getDistributionOrdersLogs(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Boolean isSuccess,
            @RequestParam(required = false) String hotelKey,
            @RequestParam(required = false) String distributionOrdersKey,
            @RequestParam(required = false) String checkInKey,
            @RequestParam(required = false) String checkOutKey,
            @RequestParam(required = false) String supplierBookingKey,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        logger.info("Getting distribution orders logs with params: appId={}, supplierId={}, traceId={}, businessType={}, isSuccess={}, page={}, size={}",
                appId, supplierId, traceId, businessType, isSuccess, page, size);

        try {
            // 验证分页参数
            size = Math.min(size, MAX_PAGE_SIZE);
            if (size <= 0) size = DEFAULT_PAGE_SIZE;
            if (page < 0) page = 0;

            // 创建分页对象，按创建时间降序
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // 构建查询条件
            Specification<DistributionOrdersLog> spec = (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (appId != null && !appId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("appId"), appId));
                }
                if (supplierId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("supplierId"), supplierId));
                }
                if (traceId != null && !traceId.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("traceId"), traceId));
                }
                if (businessType != null && !businessType.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("businessType"), businessType));
                }
                if (isSuccess != null) {
                    predicates.add(criteriaBuilder.equal(root.get("isSuccess"), isSuccess));
                }
                if (hotelKey != null && !hotelKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("hotelKey"), hotelKey));
                }
                if (distributionOrdersKey != null && !distributionOrdersKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("distributionOrdersKey"), distributionOrdersKey));
                }
                if (checkInKey != null && !checkInKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("checkInKey"), checkInKey));
                }
                if (checkOutKey != null && !checkOutKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("checkOutKey"), checkOutKey));
                }
                if (supplierBookingKey != null && !supplierBookingKey.trim().isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("supplierBookingKey"), supplierBookingKey));
                }
                if (startTime != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startTime));
                }
                if (endTime != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endTime));
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };

            // 执行查询
            Page<DistributionOrdersLog> pageResult = distributionOrdersLogRepository.findAll(spec, pageable);

            // 构建返回结果
            Map<String, Object> response = new HashMap<>();
            response.put("content", pageResult.getContent());
            response.put("totalElements", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("currentPage", pageResult.getNumber());
            response.put("pageSize", pageResult.getSize());
            response.put("hasNext", pageResult.hasNext());
            response.put("hasPrevious", pageResult.hasPrevious());

            return R.ok("获取分销商订单日志成功", response);

        } catch (Exception e) {
            logger.error("Failed to get distribution orders logs", e);
            return R.fail("获取分销商订单日志失败: " + e.getMessage());
        }
    }
}
