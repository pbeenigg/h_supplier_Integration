package com.heytrip.hotel.supplier.dto.qtech.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * QTECH酒店预订请求DTO
 * 
 * @author Pax
 */
@Data
public class QTechReservationRequest {
    
    /**
     * 接口名称
     */
    private String action = "hotel_reservation";
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 酒店ID
     */
    @JsonProperty("hotel_id")
    private String hotelId;
    
    /**
     * 搜索会话唯一ID
     */
    @JsonProperty("unique_id")
    private String uniqueId;
    
    /**
     * 房型组合唯一ID
     */
    @JsonProperty("section_unique_id")
    private String sectionUniqueId;
    
    /**
     * 代理参考号（必须唯一）
     */
    @JsonProperty("agent_ref_no")
    private String agentRefNo;
    
    /**
     * 房间详情JSON数组
     */
    @JsonProperty("roomDetails")
    private String roomDetails;
    
    /**
     * 预期价格（必须与取消规则返回的价格一致）
     */
    @JsonProperty("expected_price")
    private BigDecimal expectedPrice;
    
    /**
     * 房间详情内部类
     */
    @Data
    public static class RoomDetail {
        /**
         * 成人数
         */
        private Integer numberOfAdults;
        
        /**
         * 儿童数
         */
        private String numberOfChilds;
        
        /**
         * 房类ID
         */
        private String roomClassId;
        
        /**
         * 乘客列表
         */
        private List<Passenger> passangers;
    }
    
    /**
     * 乘客信息
     */
    @Data
    public static class Passenger {
        /**
         * 称谓
         */
        private String salutation;
        
        /**
         * 名
         */
        @JsonProperty("first_name")
        private String first_name;
        
        /**
         * 姓
         */
        @JsonProperty("last_name")
        private String last_name;
        
        /**
         * 年龄（儿童必填）
         */
        private String age;
    }
}
