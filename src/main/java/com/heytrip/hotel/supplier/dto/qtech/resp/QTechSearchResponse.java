package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * QTECH酒店搜索响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechSearchResponse {
    
    /**
     * 返回酒店数量
     */
    @JsonProperty("TotalCount")
    private Integer totalCount;
    
    /**
     * API版本
     */
    @JsonProperty("WebServiceVersion")
    private String webServiceVersion;
    
    /**
     * 请求结果
     */
    @JsonProperty("Message")
    private String message;
    
    /**
     * 酒店列表
     */
    @JsonProperty("HotelList")
    private List<Hotel> hotelList;
    
    /**
     * 本次搜索会话ID
     */
    @JsonProperty("SearchUniqueId")
    private String searchUniqueId;
    
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
        private BigDecimal latitude;
        
        /**
         * 经度
         */
        @JsonProperty("Longitude")
        private BigDecimal longitude;
        
        /**
         * 地址
         */
        @JsonProperty("Address")
        private String address;
        
        /**
         * 币种
         */
        @JsonProperty("RateCurrencyCode")
        private String rateCurrencyCode;
        
        /**
         * 最便宜房型总价
         */
        @JsonProperty("TotalCharges")
        private String totalCharges;
        
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
    }
    
    /**
     * 房型详情
     */
    @Data
    public static class RoomRate {
        /**
         * 是否可订
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
        private String numberOfAdults;
        
        /**
         * 儿童数
         */
        @JsonProperty("NumberOfChild")
        private String numberOfChild;
        
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
        @JsonProperty("Date")
        private String date;
        
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
}
