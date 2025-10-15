package com.heytrip.hotel.supplier.service;

import cn.hutool.core.util.StrUtil;
import com.heytrip.hotel.supplier.dto.ApiLogData;
import com.heytrip.hotel.supplier.entity.DistributionCallLog;
import com.heytrip.hotel.supplier.entity.DistributionOrdersLog;
import com.heytrip.hotel.supplier.repository.DistributionCallLogRepository;
import com.heytrip.hotel.supplier.repository.DistributionOrdersLogRepository;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import com.heytrip.hotel.supplier.utils.JsonCompressionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * API日志记录服务
 * 负责异步记录API调用日志和订单日志
 *
 * @author Pax
 * @since 1.0.0
 */
@Service
public class ApiLogService {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogService.class);

    @Autowired
    private DistributionCallLogRepository callLogRepository;

    @Autowired
    private DistributionOrdersLogRepository ordersLogRepository;

    /**
     * 异步记录API调用日志
     *
     * @param logData 日志数据
     * @return CompletableFuture
     */
    @Async("apiLogExecutor")
    @Transactional
    public CompletableFuture<Void> recordApiLogAsync(ApiLogData logData) {
        try {
            // 记录基础调用日志
            recordDistributionCallLog(logData);

            // 如果需要记录订单详细日志
            if (logData.isRecordOrderDetail() && logData.getOrderData() != null) {
                logger.debug("开始记录订单详细日志，traceId: {}", logData.getTraceId());
                recordDistributionOrdersLog(logData);
            } else {
                logger.warn("跳过订单详细日志记录 - recordOrderDetail: {}, orderData: {}, traceId: {}",
                        logData.isRecordOrderDetail(),
                        logData.getOrderData() != null ? "存在" : "为空",
                        logData.getTraceId());
            }

            logger.debug("API日志记录成功，traceId: {}", logData.getTraceId());
        } catch (Exception e) {
            logger.error("API日志记录失败，traceId: {}", logData.getTraceId(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 记录分销商调用日志
     */
    private void recordDistributionCallLog(ApiLogData logData) {
        try {
            DistributionCallLog callLog = new DistributionCallLog();

            // 基础信息
            callLog.setAppId(logData.getAppId());
            callLog.setSupplierId(logData.getSupplierId());
            callLog.setTraceId(logData.getTraceId());
            callLog.setApiEndpoint(logData.getApiEndpoint());
            callLog.setHttpMethod(logData.getHttpMethod());
            callLog.setClientIp(logData.getClientIp());
            callLog.setUserAgent(logData.getUserAgent());

            // 请求响应数据
            callLog.setRequestHeaders(logData.getRequestHeaders());
            callLog.setRequestParams(logData.getRequestParams());
            callLog.setRequestBody(logData.getRequestBody());
            callLog.setRequestBodyCompressed(JsonCompressionUtil.isCompressed(logData.getRequestBody()));

            callLog.setResponseHeaders(logData.getResponseHeaders());
            callLog.setResponseBody(logData.getResponseBody());
            callLog.setResponseBodyCompressed(JsonCompressionUtil.isCompressed(logData.getResponseBody()));

            callLog.setResponseStatus(logData.getResponseStatus());
            callLog.setResponseTimeMs(logData.getResponseTimeMs());

            // 业务信息
            callLog.setBusinessType(logData.getBusinessType());
            callLog.setIsSuccess(logData.getIsSuccess());
            callLog.setErrorCode(logData.getErrorCode());
            callLog.setErrorMessage(logData.getErrorMessage());

            // 提取的业务字段
            if (logData.getExtractedFields() != null) {
                Map<String, Object> fields = logData.getExtractedFields();
                callLog.setHotelKey(HeyUtil.getStringValue(fields, "hotelId"));
                callLog.setCheckInKey(HeyUtil.getStringValue(fields, "checkInDate"));
                callLog.setCheckOutKey(HeyUtil.getStringValue(fields, "checkOutDate"));
                callLog.setDistributionOrdersKey(HeyUtil.getStringValue(fields, "distributorOrderId"));
            }

            callLogRepository.save(callLog);
        } catch (Exception e) {
            logger.error("保存分销商调用日志失败，traceId: {}", logData.getTraceId(), e);
            throw e;
        }
    }

    /**
     * 记录分销商订单日志
     */
    private void recordDistributionOrdersLog(ApiLogData logData) {
        try {
            DistributionOrdersLog ordersLog = new DistributionOrdersLog();

            // 基础信息
            ordersLog.setAppId(logData.getAppId());
            ordersLog.setSupplierId(logData.getSupplierId());
            ordersLog.setTraceId(logData.getTraceId());
            ordersLog.setBusinessType(logData.getBusinessType());
            ordersLog.setIsSuccess(logData.getIsSuccess());
            ordersLog.setErrorCode(logData.getErrorCode());
            ordersLog.setErrorMessage(logData.getErrorMessage());

            // 订单详细信息
            if (logData.getOrderData() != null) {
                var orderData = logData.getOrderData();
                ordersLog.setHotelKey(orderData.getHotelKey());
                ordersLog.setCheckInKey(orderData.getCheckInKey());
                ordersLog.setCheckOutKey(orderData.getCheckOutKey());
                ordersLog.setRoomKey(orderData.getRoomKey());
                ordersLog.setRateKey(orderData.getRateKey());

                if (orderData.getCheckInKey() != null && orderData.getCheckOutKey() != null) {
                    try {
                        ordersLog.setNights(HeyUtil.daysBetween(HeyUtil.parseToLocalDateTime(orderData.getCheckInKey()).toLocalDate(), HeyUtil.parseToLocalDateTime(orderData.getCheckOutKey()).toLocalDate()));
                    } catch (Exception e) {
                        logger.warn("计算入住晚数失败，traceId: {}, checkInKey: {}, checkOutKey: {}",
                                logData.getTraceId(), orderData.getCheckInKey(), orderData.getCheckOutKey(), e);
                    }
                }

                if(StrUtil.isNotBlank(orderData.getOccupancy())){
                    ordersLog.setOccupancy(orderData.getOccupancy());
                    ordersLog.setGuests(HeyUtil.calculateTotalGuests(orderData.getOccupancy()));
                }

                ordersLog.setRooms(orderData.getRooms());
                ordersLog.setCurrency(orderData.getCurrency());
                ordersLog.setNational(orderData.getNational());
                ordersLog.setTotalAmount(orderData.getTotalAmount());
                ordersLog.setDistributionOrdersKey(orderData.getDistributionOrdersKey());
                ordersLog.setSupplierBookingKey(orderData.getSupplierBookingKey());
                ordersLog.setBookingStatus(orderData.getBookingStatus());
                ordersLog.setOriginalRequest(orderData.getOriginalRequest());
                ordersLog.setOriginalResponse(orderData.getOriginalResponse());
            }

            ordersLogRepository.save(ordersLog);
        } catch (Exception e) {
            logger.error("保存分销商订单日志失败，traceId: {}", logData.getTraceId(), e);
            throw e;
        }
    }

    /**
     * 同步记录API日志（用于测试或特殊场景）
     */
    @Transactional
    public void recordApiLogSync(ApiLogData logData) {
        recordDistributionCallLog(logData);
        if (logData.isRecordOrderDetail() && logData.getOrderData() != null) {
            recordDistributionOrdersLog(logData);
        }
    }
}
