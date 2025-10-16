package com.heytrip.hotel.supplier.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.adapter.SupplierAdapter;
import com.heytrip.hotel.supplier.adapter.SupplierAdapterManager;
import com.heytrip.hotel.supplier.annotation.ApiLog;
import com.heytrip.hotel.supplier.config.ApiLogProperties;
import com.heytrip.hotel.supplier.dto.ApiLogData;
import com.heytrip.hotel.supplier.dto.OrderLogData;
import com.heytrip.hotel.supplier.exception.SupplierException;
import com.heytrip.hotel.supplier.service.ApiLogService;
import com.heytrip.hotel.supplier.utils.ApiLogExtractUtil;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import com.heytrip.hotel.supplier.utils.JsonCompressionUtil;
import com.heytrip.hotel.supplier.utils.TraceIdHolder;
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
        String traceId = generateTraceId(request, response);
        MDC.put("traceId", traceId);

        // 将traceId设置到Request和Response的header中，便于链路追踪
        setTraceIdToHeaders(request, response, traceId);

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

                // 如果未能通过请求头或体提取到supplierId，尝试从提取的字段中获取
                if(logData.getSupplierId() == null) {
                    Map<String, Object> fields = logData.getExtractedFields();
                    String supplierType = fields != null ? HeyUtil.getStringValue(fields, "supplierType") : "";
                    if (StringUtils.hasText(supplierType)) {
                        SupplierAdapter supplierAdapter = supplierAdapterManager.getAdapterByName(supplierType);
                        if (supplierAdapter != null) {
                            logData.setSupplierId(supplierAdapter.getSupplierId());
                        }
                    }
                }

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
                TraceIdHolder.clear(); // 清理ThreadLocal，避免内存泄漏
            }
        }
    }

    /**
     * 生成traceId
     */
    private String generateTraceId(HttpServletRequest request, HttpServletResponse response) {
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
     * 将traceId设置到Request和Response的header中
     * 这样可以在后续的HTTP调用中传递traceId，实现完整的链路追踪
     */
    private void setTraceIdToHeaders(HttpServletRequest request, HttpServletResponse response, String traceId) {
        try {
            // 设置到Response header中，前端可以获取到
            if (response != null && !response.isCommitted()) {
                response.setHeader("X-Trace-Id", traceId);
                response.setHeader("traceId", traceId); // 兼容性header
            }

            // 将traceId设置到Request的attribute中，供后续的HTTP客户端使用
            if (request != null) {
                request.setAttribute("X-Trace-Id", traceId);
                request.setAttribute("traceId", traceId);
            }

            // 同时设置到ThreadLocal中，供HTTP客户端工具类使用
            TraceIdHolder.setTraceId(traceId);

            logger.debug("TraceId已设置到headers: {}", traceId);
        } catch (Exception e) {
            logger.warn("设置TraceId到headers失败: {}", traceId, e);
        }
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
        logData.setClientIp(HeyUtil.getClientIp(request));
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

            // 智能异常识别和转换
            ExceptionInfo exceptionInfo = analyzeException(exception);
            logData.setErrorCode(exceptionInfo.getErrorCode());
            logData.setErrorMessage(exceptionInfo.getErrorMessage());

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
        mapping.put("hotelId", "HotelId");

        // 日期相关字段映射
        mapping.put("checkInDate", "CheckInDate");
        mapping.put("checkOutDate", "CheckOutDate");
        mapping.put("checkIn", "CheckInDate");
        mapping.put("checkOut", "CheckOutDate");
        mapping.put("checkInKey", "CheckInDate");
        mapping.put("checkOutKey", "CheckOutDate");

        // 订单相关字段映射
        mapping.put("distributorOrderId", "DistributorOrderId");
        mapping.put("distributorOrdersKey", "DistributorOrderId");
        mapping.put("supplierBookingKey", "supplierOrderId");
        mapping.put("supplierOrderId", "SupplierOrderId");

        // 房间相关字段映射
        mapping.put("roomId", "RoomId");
        mapping.put("ratePlanId", "RatePlanId");
        mapping.put("rooms", "RoomNum");
        mapping.put("roomNum", "RoomNum");

        // 价格相关字段映射
        mapping.put("totalAmount", "SalePrice");
        mapping.put("totalAmount", "TotalPrice");
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

    /**
     * 智能分析异常并生成错误信息
     * 根据异常类型动态转换成相应的业务错误代码
     */
    private ExceptionInfo analyzeException(Throwable exception) {
        String errorCode;
        String errorMessage = exception.getMessage();

        // 如果已经是BusinessException，直接使用其错误信息
        if (exception instanceof SupplierException) {
            SupplierException bizEx = (SupplierException) exception;
            errorCode = "BIZ_" + bizEx.getCode() + "_" + bizEx.getBizCode();
            return new ExceptionInfo(errorCode, errorMessage, bizEx.getData());
        }

        // 根据异常类型进行智能识别和转换
        Class<?> exceptionClass = exception.getClass();
        String exceptionName = exceptionClass.getSimpleName();

        switch (exceptionName) {
            case "IllegalArgumentException":
                errorCode = "PARAM_INVALID";
                errorMessage = "参数错误: " + (errorMessage != null ? errorMessage : "无效的参数");
                break;

            case "NullPointerException":
                errorCode = "NULL_POINTER";
                errorMessage = "空指针异常: " + (errorMessage != null ? errorMessage : "访问了空对象");
                break;

            case "NumberFormatException":
                errorCode = "NUMBER_FORMAT";
                errorMessage = "数字格式错误: " + (errorMessage != null ? errorMessage : "无法解析数字");
                break;

            case "JsonProcessingException":
            case "JsonParseException":
            case "JsonEOFException":
                errorCode = "JSON_PARSE_ERROR";
                errorMessage = "JSON解析错误: " + (errorMessage != null ? errorMessage : "JSON格式不正确");
                break;

            case "HttpClientErrorException":
                errorCode = "HTTP_CLIENT_ERROR";
                errorMessage = "HTTP客户端错误: " + (errorMessage != null ? errorMessage : "请求失败");
                break;
            case "WebClientRequestException":
                errorCode = "HTTP_CLIENT_REQUEST_ERROR";
                errorMessage = "HTTP客户端请求错误: " + (errorMessage != null ? errorMessage : "请求失败");
                break;

            case "ConnectTimeoutException":
            case "SocketTimeoutException":
                errorCode = "TIMEOUT_ERROR";
                errorMessage = "超时错误: " + (errorMessage != null ? errorMessage : "连接或读取超时");
                break;

            case "DataAccessException":
            case "SQLException":
                errorCode = "DATABASE_ERROR";
                errorMessage = "数据库错误: " + (errorMessage != null ? errorMessage : "数据库操作失败");
                break;

            case "ValidationException":
                errorCode = "VALIDATION_ERROR";
                errorMessage = "验证错误: " + (errorMessage != null ? errorMessage : "数据验证失败");
                break;

            case "SecurityException":
                errorCode = "SECURITY_ERROR";
                errorMessage = "安全错误: " + (errorMessage != null ? errorMessage : "权限不足或安全检查失败");
                break;

            case "ClassCastException":
                errorCode = "TYPE_CAST_ERROR";
                errorMessage = "类型转换错误: " + (errorMessage != null ? errorMessage : "对象类型转换失败");
                break;

            case "ConcurrentModificationException":
                errorCode = "CONCURRENT_ERROR";
                errorMessage = "并发修改错误: " + (errorMessage != null ? errorMessage : "并发访问冲突");
                break;

            default:
                // 处理未知异常类型
                if (exceptionName.contains("Business")) {
                    errorCode = "BUSINESS_ERROR";
                    errorMessage = "业务异常: " + (errorMessage != null ? errorMessage : "业务逻辑处理失败");
                } else if (exceptionName.contains("Runtime")) {
                    errorCode = "RUNTIME_ERROR";
                    errorMessage = "运行时异常: " + (errorMessage != null ? errorMessage : "运行时发生错误");
                } else if (exceptionName.contains("IO")) {
                    errorCode = "IO_ERROR";
                    errorMessage = "IO异常: " + (errorMessage != null ? errorMessage : "输入输出操作失败");
                } else {
                    errorCode = "UNKNOWN_ERROR";
                    errorMessage = "未知错误: " + exceptionName + " - " + (errorMessage != null ? errorMessage : "系统发生未知错误");
                }
                break;
        }

        // 记录异常转换日志
        logger.debug("异常转换: {} -> {}, 原始消息: {}, 转换后消息: {}",
                    exceptionName, errorCode, exception.getMessage(), errorMessage);

        return new ExceptionInfo(errorCode, errorMessage, null);
    }

    /**
     * 异常信息封装类
     */
    private static class ExceptionInfo {
        private final String errorCode;
        private final String errorMessage;
        private final Object data;

        public ExceptionInfo(String errorCode, String errorMessage, Object data) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.data = data;
        }

        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public Object getData() { return data; }
    }
}
