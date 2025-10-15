package com.heytrip.hotel.supplier.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * TraceId持有者工具类
 * 使用ThreadLocal存储当前线程的traceId，便于在整个请求链路中传递
 *
 * @author Pax
 * @since 1.0.0
 */
public class TraceIdHolder {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdHolder.class);

    /**
     * TraceId header名称常量
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_HEADER_LEGACY = "traceId";

    /**
     * 使用ThreadLocal存储traceId
     */
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的traceId
     *
     * @param traceId 追踪ID
     */
    public static void setTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            TRACE_ID_HOLDER.set(traceId);
            logger.debug("设置TraceId到ThreadLocal: {}", traceId);
        }
    }

    /**
     * 获取当前线程的traceId
     *
     * @return 追踪ID，如果不存在则返回null
     */
    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 获取当前线程的traceId，如果不存在则生成新的
     *
     * @return 追踪ID
     */
    public static String getOrGenerateTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (!StringUtils.hasText(traceId)) {
            traceId = ApiLogExtractUtil.generateTraceId();
            setTraceId(traceId);
            logger.debug("生成新的TraceId: {}", traceId);
        }
        return traceId;
    }

    /**
     * 清除当前线程的traceId
     * 建议在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        String traceId = TRACE_ID_HOLDER.get();
        TRACE_ID_HOLDER.remove();
        if (StringUtils.hasText(traceId)) {
            logger.debug("清除ThreadLocal中的TraceId: {}", traceId);
        }
    }

    /**
     * 检查当前线程是否有traceId
     *
     * @return true表示有traceId，false表示没有
     */
    public static boolean hasTraceId() {
        return StringUtils.hasText(TRACE_ID_HOLDER.get());
    }
}
