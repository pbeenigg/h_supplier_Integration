package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * QTECH酒店搜索响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechSearchResponse extends QTechBaseResponse {
    
    /**
     * 酒店列表（仅在成功时存在）
     */
    @JsonProperty("HotelList")
    private List<Hotel> hotelList;
    
    /**
     * 酒店信息
     */
    @Data
    public static class Hotel {
        /**
         * 运行时酒店ID
         */
        @JsonProperty("HotelId")
        private String hotelId;
        
        /**
         * 静态酒店ID
         */
        @JsonProperty("LocalHotelId")
        private String localHotelId;
        
        /**
         * 酒店名称
         */
        @JsonProperty("HotelName")
        private String hotelName;
        
        /**
         * 星级
         */
        @JsonProperty("PropertyRating")
        private String propertyRating;
        
        /**
         * 是否可订
         */
        @JsonProperty("Available")
        private String available;
        
        /**
         * 纬度
         */
        @JsonProperty("Latitude")
        private String latitude;
        
        /**
         * 经度
         */
        @JsonProperty("Longitude")
        private String longitude;
        
        /**
         * 地址
         */
        @JsonProperty("Address")
        private String address;
        
        /**
         * 酒店缩略图URL
         */
        @JsonProperty("ThumbNailUrl")
        private String thumbNailUrl;
        
        /**
         * 币种
         */
        @JsonProperty("RateCurrencyCode")
        private String rateCurrencyCode;
        
        /**
         * 最便宜房型总价
         */
        @JsonProperty("TotalCharges")
        private BigDecimal totalCharges;
        
        /**
         * 酒店房型列表
         */
        @JsonProperty("HotelProperty")
        private List<HotelProperty> hotelProperty;
    }
    
    /**
     * 酒店房型组合
     */
    @Data
    public static class HotelProperty {
        /**
         * 房型组合总价
         */
        @JsonProperty("DisplayRoomRate")
        private BigDecimal displayRoomRate;
        
        /**
         * 类型
         */
        @JsonProperty("Type")
        private String type;
        
        /**
         * 房型组合唯一ID
         */
        @JsonProperty("SectionUniqueId")
        private String sectionUniqueId;
        
        /**
         * 房型列表
         */
        @JsonProperty("RoomRates")
        private List<RoomRate> roomRates;
        
        /**
         * 房间详情列表
         */
        @JsonProperty("RoomDetails")
        private List<Object> roomDetails;
        
        /**
         * 是否可退款
         */
        @JsonProperty("Refundable")
        private Boolean refundable;
        
        /**
         * 取消规则信息
         */
        @JsonProperty("Policies")
        private Policies policies;
    }
    
    /**
     * 房型详情
     */
    @Data
    public static class RoomRate {
        /**
         * 是否可订  1 - 可订，0 - 不可订
         */
        @JsonProperty("Available")
        private Integer available;
        
        /**
         * 房间数
         */
        @JsonProperty("NumberOfRooms")
        private Integer numberOfRooms;
        
        /**
         * 成人数
         */
        @JsonProperty("NumberOfAdults")
        private Integer numberOfAdults;
        
        /**
         * 儿童数
         */
        @JsonProperty("NumberOfChild")
        private String numberOfChild;
        
        /**
         * 儿童年龄列表 [[ 5, 3 ]]
         */
        @JsonProperty("ChildAges")
        private String[][]  childAges;
        
        /**
         * 房型价格
         */
        @JsonProperty("RoomRate")
        private BigDecimal roomRate;
        
        /**
         * 房型描述
         */
        @JsonProperty("RoomType")
        private String roomType;
        
        /**
         * 房型类别
         */
        @JsonProperty("RoomCategory")
        private String roomCategory;
        
        /**
         * 餐型
         */
        @JsonProperty("MealBasis")
        private String mealBasis;
        
        /**
         * 餐型代码
         */
        @JsonProperty("MealCode")
        private String mealCode;
        
        /**
         * 备注
         */
        @JsonProperty("Note")
        private String note;
        
        /**
         * 具体房类ID
         */
        @JsonProperty("ClassUniqueId")
        private String classUniqueId;
        
        /**
         * 日价明细
         */
        @JsonProperty("RateBreakup")
        private List<RateBreakup> rateBreakup;
    }
    
    /**
     * 日价明细
     */
    @Data
    public static class RateBreakup {
        /**
         * 日期
         */
        @JsonFormat(pattern = "dd-MM-yyyy")
        @JsonProperty("Date")
        private LocalDate date;
        
        /**
         * 星期
         */
        @JsonProperty("Day")
        private String day;
        
        /**
         * 当日价格
         */
        @JsonProperty("DisplayNightlyRate")
        private BigDecimal displayNightlyRate;
    }
    
    /**
     * 政策信息
     */
    @Data
    public static class Policies {
        /**
         * 取消政策列表
         */
        @JsonProperty("CancellationPolicy")
        private List<CancellationPolicy> cancellationPolicy;
    }
    
    /**
     * 取消政策
     */
    @Data
    public static class CancellationPolicy {
        /**
         * 开始时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss Z")
        @JsonProperty("Start")
        private OffsetDateTime start;
        
        /**
         * 结束时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss Z")
        @JsonProperty("End")
        private OffsetDateTime end;
        
        /**
         * 取消费用
         */
        @JsonProperty("Charges")
        private Double charges;
    }
}
