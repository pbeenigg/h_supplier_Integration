package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH API通用响应基类
 * <p>
 * 用于处理所有QTECH API的通用响应字段，包括成功和错误响应
 * 
 * @author Pax
 */
@Data
public class QTechBaseResponse {

    /**
     * 状态
     */
    @JsonProperty("Status")
    private String status;
    
    /**
     * 返回数据数量（成功时为实际数量，失败时通常为0）
     */
    @JsonProperty("TotalCount")
    private Integer totalCount;
    
    /**
     * API版本
     */
    @JsonProperty("WebServiceVersion")
    private String webServiceVersion;
    
    /**
     * 请求结果状态（success/fail）
     */
    @JsonProperty("Message")
    private String message;
    
    /**
     * 开始时间
     */
    @JsonProperty("StartTime")
    private String startTime;
    
    /**
     * 结束时间
     */
    @JsonProperty("EndTime")
    private String endTime;
    
    /**
     * 本次请求会话ID
     * 每个搜索唯一ID只能用于一次预订，并且从搜索时间起20分钟内有效
     */
    @JsonProperty("SearchUniqueId")
    private String searchUniqueId;
    
    /**
     * 错误详细信息（仅在失败时存在）
     */
    @JsonProperty("MessageInfo")
    private String messageInfo;


    /**
     * 是否登录
     */
    @JsonProperty("Login")
    private String login;
    
    /**
     * 判断响应是否成功
     * 
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(message);
    }
    
    /**
     * 判断响应是否失败
     * 
     * @return true表示失败，false表示成功
     */
    public boolean isFailed() {
        return !isSuccess();
    }
    
    /**
     * 获取错误信息
     * 
     * @return 错误信息，如果没有则返回默认消息
     */
    public String getErrorMessage() {
        if (messageInfo != null && !messageInfo.trim().isEmpty()) {
            return messageInfo;
        }
        if (isFailed()) {
            return "请求失败，状态: " + message;
        }
        return null;
    }
}
