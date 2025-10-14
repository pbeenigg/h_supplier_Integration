package com.heytrip.hotel.supplier.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.adapter.SupplierAdapter;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.annotation.ApiLog;
import com.heytrip.hotel.supplier.config.ApiLogProperties;
import com.heytrip.hotel.supplier.dto.ApiLogData;
import com.heytrip.hotel.supplier.dto.OrderLogData;
import com.heytrip.hotel.supplier.service.ApiLogService;
import com.heytrip.hotel.supplier.utils.ApiLogExtractUtil;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import com.heytrip.hotel.supplier.utils.JsonCompressionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * API日志记录切面
 * 拦截带有@ApiLog注解的方法，自动记录API调用日志
 *
 * @author Pax
 * @since 1.0.0
 */
@Aspect
@Component
@Order(1)
public class ApiLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogAspect.class);

    @Autowired
    private ApiLogService apiLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiLogProperties apiLogProperties;

    @Value("${app.authorization.app-id:heytrip_supplier_integration_pax}")
    private String defaultAppId;


    @Autowired
    private SupplierAdapterManager supplierAdapterManager;


    /**
     * 拦截所有带@ApiLog注解的方法
     */
    @Around("@annotation(apiLog)")
    public Object around(ProceedingJoinPoint joinPoint, ApiLog apiLog) throws Throwable {
        // 检查日志功能是否启用
        if (!apiLogProperties.isEnabled() || !apiLog.enabled()) {
            return joinPoint.proceed();
        }

        // 获取HTTP请求和响应对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.warn("无法获取HTTP请求上下文，跳过日志记录");
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        // 生成并设置traceId
        String traceId = generateTraceId(request);
        MDC.put("traceId", traceId);

        long startTime = System.currentTimeMillis();
        ApiLogData logData = initializeLogData(request, traceId, apiLog);

        Object result = null;
        try {
            // 记录请求信息
            recordRequestInfo(logData, request, joinPoint);

            // 执行目标方法
            result = joinPoint.proceed();

            // 记录响应信息
            recordResponseInfo(logData, response, result, startTime);

            return result;
        } catch (Throwable e) {
            recordExceptionInfo(logData, e, startTime);
            throw e;
        } finally {
            try {
                // 提取业务字段
                extractBusinessFields(logData, request, result, apiLog.extractFields());

                // 构建订单日志数据
                if (apiLog.recordOrderDetail()) {
                    buildOrderLogData(logData, request, result);
                }

                // 异步记录日志
                apiLogService.recordApiLogAsync(logData);
            } catch (Exception e) {
                logger.error("记录API日志失败，traceId: {}", traceId, e);
            } finally {
                MDC.remove("traceId");
            }
        }
    }

    /**
     * 生成traceId
     */
    private String generateTraceId(HttpServletRequest request) {
        // 优先使用请求头中的traceId
        String traceId = request.getHeader("X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader("traceId");
        }

        // 如果没有则生成新的
        if (!StringUtils.hasText(traceId)) {
            traceId = ApiLogExtractUtil.generateTraceId();
        }

        return traceId;
    }

    /**
     * 初始化日志数据对象
     */
    private ApiLogData initializeLogData(HttpServletRequest request, String traceId, ApiLog apiLog) {
        ApiLogData logData = new ApiLogData();

        // 基础信息
        logData.setTraceId(traceId);
        logData.setAppId(extractAppId(request));
        logData.setApiEndpoint(request.getRequestURI());
        logData.setHttpMethod(request.getMethod());
        logData.setBusinessType(apiLog.businessType());
        logData.setClientIp(ApiLogExtractUtil.getClientIp(request));
        logData.setUserAgent(request.getHeader("User-Agent"));
        logData.setRecordOrderDetail(apiLog.recordOrderDetail());

        // 提取供应商标识
        String supplierType = extractSupplierType(request);
        SupplierAdapter supplierAdapter = supplierAdapterManager.getAdapterByName(supplierType);
        if (supplierAdapter != null) {
            logData.setSupplierId(supplierAdapter.getSupplierId());
        }
        return logData;
    }

    /**
     * 记录请求信息
     */
    private void recordRequestInfo(ApiLogData logData, HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        try {
            // 记录请求头
            logData.setRequestHeaders(ApiLogExtractUtil.extractRequestHeaders(request));

            // 记录请求参数
            logData.setRequestParams(ApiLogExtractUtil.extractRequestParams(request));

            // 记录请求体
            String requestBody = extractRequestBody(request, joinPoint);

            // 使用智能内容处理（压缩优先）
            String processedRequestBody = ApiLogExtractUtil.processContent(
                ApiLogExtractUtil.maskSensitiveData(requestBody),
                apiLogProperties.getMaxContentLength(),
                apiLogProperties.getCompression().isEnabled(),
                apiLogProperties.getCompression().getThreshold(),
                apiLogProperties.getCompression().getCompressionAlgorithm(),
                apiLogProperties.getCompression().getLevel()
            );

            logData.setRequestBody(processedRequestBody);

        } catch (Exception e) {
            logger.warn("记录请求信息失败，traceId: {}", logData.getTraceId(), e);
        }
    }

    /**
     * 记录响应信息
     */
    private void recordResponseInfo(ApiLogData logData, HttpServletResponse response, Object result, long startTime) {
        try {
            long responseTime = System.currentTimeMillis() - startTime;
            logData.setResponseTimeMs(responseTime);

            if (response != null) {
                logData.setResponseStatus(response.getStatus());
            }

            // 序列化响应结果
            if (result != null) {
                String responseBody = objectMapper.writeValueAsString(result);

                // 使用智能内容处理（压缩优先）
                String processedResponseBody = ApiLogExtractUtil.processContent(
                    ApiLogExtractUtil.maskSensitiveData(responseBody),
                    apiLogProperties.getMaxContentLength(),
                    apiLogProperties.getCompression().isEnabled(),
                    apiLogProperties.getCompression().getThreshold(),
                    apiLogProperties.getCompression().getCompressionAlgorithm(),
                    apiLogProperties.getCompression().getLevel()
                );

                logData.setResponseBody(processedResponseBody);
            }

            logData.setIsSuccess(true);

        } catch (Exception e) {
            logger.warn("记录响应信息失败，traceId: {}", logData.getTraceId(), e);
        }
    }

    /**
     * 记录异常信息
     */
    private void recordExceptionInfo(ApiLogData logData, Throwable exception, long startTime) {
        try {
            long responseTime = System.currentTimeMillis() - startTime;
            logData.setResponseTimeMs(responseTime);
            logData.setIsSuccess(false);
            logData.setErrorCode(exception.getClass().getSimpleName());
            logData.setErrorMessage(exception.getMessage());
            logData.setResponseStatus(500);

        } catch (Exception e) {
            logger.warn("记录异常信息失败，traceId: {}", logData.getTraceId(), e);
        }
    }

    /**
     * 提取业务字段
     */
    private void extractBusinessFields(ApiLogData logData, HttpServletRequest request, Object result, String[] extractFields) {
        try {
            Map<String, Object> extractedFields = null;

            // 从请求体中提取（需要先解压缩）
            if (StringUtils.hasText(logData.getRequestBody())) {
                String requestBodyForExtraction = decompressIfNeeded(logData.getRequestBody());
                extractedFields = ApiLogExtractUtil.extractFieldsFromJson(requestBodyForExtraction, extractFields);

                // 如果标准字段名提取失败，尝试使用字段名映射
                if (extractedFields == null || extractedFields.isEmpty()) {
                    logger.debug("标准字段名提取失败，尝试使用字段名映射，traceId: {}", logData.getTraceId());
                    extractedFields = extractFieldsWithMapping(requestBodyForExtraction, extractFields);
                }
            }

            // 从响应中提取（需要先解压缩）
            if (StringUtils.hasText(logData.getResponseBody())) {
                String responseBodyForExtraction = decompressIfNeeded(logData.getResponseBody());
                Map<String, Object> responseFields = ApiLogExtractUtil.extractFieldsFromJson(responseBodyForExtraction, extractFields);

                // 如果标准字段名提取失败，尝试使用字段名映射
                if (responseFields == null || responseFields.isEmpty()) {
                    logger.debug("响应标准字段名提取失败，尝试使用字段名映射，traceId: {}", logData.getTraceId());
                    responseFields = extractFieldsWithMapping(responseBodyForExtraction, extractFields);
                }

                if (extractedFields == null) {
                    extractedFields = responseFields;
                } else if (responseFields != null) {
                    extractedFields.putAll(responseFields);
                }
            }

            logData.setExtractedFields(extractedFields);

            if (extractedFields != null && !extractedFields.isEmpty()) {
                logger.debug("字段提取成功，数量: {}, 字段: {}, traceId: {}",
                           extractedFields.size(), extractedFields.keySet(), logData.getTraceId());
            } else {
                logger.warn("字段提取失败，无提取到任何字段，traceId: {}", logData.getTraceId());
            }

        } catch (Exception e) {
            logger.warn("提取业务字段失败，traceId: {}", logData.getTraceId(), e);
        }
    }

    /**
     * 使用字段名映射提取字段
     * 处理JSON字段名与配置字段名不匹配的情况
     */
    private Map<String, Object> extractFieldsWithMapping(String jsonContent, String[] extractFields) {
        try {
            // 创建字段名映射表
            Map<String, String> fieldMapping = createFieldMapping();

            // 将配置的字段名转换为实际的JSON字段名
            String[] actualJsonFields = new String[extractFields.length];
            for (int i = 0; i < extractFields.length; i++) {
                String configField = extractFields[i];
                String actualField = fieldMapping.getOrDefault(configField, configField);
                actualJsonFields[i] = actualField;
                logger.debug("字段映射: {} -> {}", configField, actualField);
            }

            // 使用实际的JSON字段名提取
            Map<String, Object> rawFields = ApiLogExtractUtil.extractFieldsFromJson(jsonContent, actualJsonFields);

            if (rawFields == null || rawFields.isEmpty()) {
                return null;
            }

            // 将结果转换回配置的字段名
            Map<String, Object> mappedFields = new HashMap<>();
            for (int i = 0; i < extractFields.length; i++) {
                String configField = extractFields[i];
                String actualField = actualJsonFields[i];
                Object value = rawFields.get(actualField);
                if (value != null) {
                    mappedFields.put(configField, value);
                }
            }

            logger.debug("字段映射提取完成，原始字段: {}, 映射后字段: {}", rawFields.keySet(), mappedFields.keySet());
            return mappedFields;

        } catch (Exception e) {
            logger.warn("字段名映射提取失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 创建字段名映射表
     * 将注解中配置的字段名映射到实际JSON中的字段名
     */
    private Map<String, String> createFieldMapping() {
        Map<String, String> mapping = new HashMap<>();

        // 酒店相关字段映射
        mapping.put("hotelKey", "HotelId");
        mapping.put("hotelId", "HotelId");

        // 日期相关字段映射
        mapping.put("checkInKey", "CheckInDate");
        mapping.put("checkInDate", "CheckInDate");
        mapping.put("checkOutKey", "CheckOutDate");
        mapping.put("checkOutDate", "CheckOutDate");

        // 订单相关字段映射
        mapping.put("distributionOrdersKey", "DistributorOrderId");
        mapping.put("distributorOrderId", "DistributorOrderId");
        mapping.put("supplierBookingKey", "SupplierBookingKey");
        mapping.put("supplierOrderId", "SupplierBookingKey");

        // 房间相关字段映射
        mapping.put("roomKey", "RoomId");
        mapping.put("roomId", "RoomId");
        mapping.put("rateKey", "RatePlanId");
        mapping.put("ratePlanId", "RatePlanId");
        mapping.put("rooms", "RoomNum");
        mapping.put("roomNum", "RoomNum");

        // 价格相关字段映射
        mapping.put("totalAmount", "SalePrice");
        mapping.put("salePrice", "SalePrice");
        mapping.put("totalPrice", "TotalPrice");

        // 其他字段映射
        mapping.put("currency", "Currency");
        mapping.put("national", "Nationality");
        mapping.put("nationality", "Nationality");
        mapping.put("occupancy", "Occupancy");
        mapping.put("supplierType", "SupplierType");
        mapping.put("guests", "Guests");
        mapping.put("nights", "Nights");

        logger.debug("字段映射表创建完成，映射数量: {}", mapping.size());
        return mapping;
    }

    /**
     * 如果内容已压缩则解压缩，否则返回原内容
     * 用于业务字段提取时获取原始JSON内容
     */
    private String decompressIfNeeded(String content) {
        try {
            if (StringUtils.hasText(content) && JsonCompressionUtil.isCompressed(content)) {
                logger.debug("检测到压缩内容，开始解压缩用于字段提取");
                return JsonCompressionUtil.decompress(content);
            }
            return content;
        } catch (Exception e) {
            logger.warn("解压缩失败，使用原始内容进行字段提取: {}", e.getMessage());
            return content;
        }
    }

    /**
     * 构建订单日志数据
     */
    private void buildOrderLogData(ApiLogData logData, HttpServletRequest request, Object result) {
        try {
            logger.debug("开始构建订单日志数据，traceId: {}", logData.getTraceId());

            OrderLogData orderData = new OrderLogData();

            // 从提取的字段中获取订单信息
            Map<String, Object> fields = logData.getExtractedFields();
            if (fields != null && !fields.isEmpty()) {
                logger.debug("提取的字段数量: {}, 字段列表: {}", fields.size(), fields.keySet());

                // 使用安全的类型转换和空值检查
                orderData.setHotelKey(HeyUtil.getStringValue(fields, "hotelId"));
                orderData.setCheckInKey(HeyUtil.getStringValue(fields, "checkInDate"));
                orderData.setCheckOutKey(HeyUtil.getStringValue(fields, "checkOutDate"));
                orderData.setDistributionOrdersKey(HeyUtil.getStringValue(fields, "distributorOrderId"));
                orderData.setSupplierBookingKey(HeyUtil.getStringValue(fields, "supplierOrderId"));
                orderData.setRoomKey(HeyUtil.getStringValue(fields, "roomId"));
                orderData.setRateKey(HeyUtil.getStringValue(fields, "ratePlanId"));
                orderData.setCurrency(HeyUtil.getStringValue(fields, "currency"));
                orderData.setNational(HeyUtil.getStringValue(fields, "national"));
                orderData.setTotalAmount(HeyUtil.getBigDecimalValue(fields, "salePrice"));
                orderData.setOccupancy(HeyUtil.getStringValue(fields, "occupancy"));
                orderData.setRooms(HeyUtil.getIntValue(fields, "roomNum", 1));

                logger.debug("订单数据构建完成 - hotelKey: {}, distributionOrdersKey: {}, totalAmount: {}",
                           orderData.getHotelKey(), orderData.getDistributionOrdersKey(), orderData.getTotalAmount());
            } else {
                logger.warn("提取的字段为空，无法构建完整的订单数据，traceId: {}", logData.getTraceId());
            }

            // 设置原始请求和响应
            orderData.setOriginalRequest(logData.getRequestBody());
            orderData.setOriginalResponse(logData.getResponseBody());

            logData.setOrderData(orderData);
            logger.debug("订单日志数据设置完成，orderData: {}", orderData != null ? "已创建" : "为空");

        } catch (Exception e) {
            logger.error("构建订单日志数据失败，traceId: {}", logData.getTraceId(), e);
        }
    }

    /**
     * 提取appId
     */
    private String extractAppId(HttpServletRequest request) {
        String appId = request.getHeader("app");
        if (!StringUtils.hasText(appId)) {
            appId = request.getHeader("X-App-Id");
        }
        if (!StringUtils.hasText(appId)) {
            appId = request.getHeader("appId");
        }
        return StringUtils.hasText(appId) ? appId : defaultAppId;
    }

    /**
     * 提取supplierType
     */
    private String extractSupplierType(HttpServletRequest request) {
        try {
            String supplierType = request.getHeader("supplierType");
            if (!StringUtils.hasText(supplierType)) {
                supplierType = request.getParameter("supplierType");
            }
            return StringUtils.hasText(supplierType) ? supplierType : null;
        } catch (NumberFormatException e) {
            logger.warn("解析supplierType失败: {}", request.getHeader("supplierType"));
            return null;
        }
    }

    /**
     * 提取请求体内容
     */
    private String extractRequestBody(HttpServletRequest request, ProceedingJoinPoint joinPoint) {
        try {
            // 尝试从方法参数中获取请求体
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    if (arg != null && !isPrimitiveOrWrapper(arg.getClass()) &&
                            !(arg instanceof HttpServletRequest) && !(arg instanceof HttpServletResponse)) {
                        return objectMapper.writeValueAsString(arg);
                    }
                }
            }
            return "";
        } catch (Exception e) {
            logger.warn("提取请求体失败", e);
            return "";
        }
    }

    /**
     * 判断是否为基本类型或包装类型
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Integer.class || clazz == Long.class ||
                clazz == Double.class || clazz == Float.class ||
                clazz == Boolean.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Short.class;
    }
}
