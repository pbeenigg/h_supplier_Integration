package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * QTECH酒店详情响应DTO
 * 根据实际API响应结构更新
 * 
 * @author Pax
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QTechHotelDetailResponse extends QTechBaseResponse {
    

    /**
     * 酒店ID
     */
    @JsonProperty("HotelId")
    private String hotelId;
    
    /**
     * 酒店名称
     */
    @JsonProperty("HotelName")
    private String hotelName;
    
    /**
     * 酒店描述
     */
    @JsonProperty("Description")
    private String description;
    
    /**
     * 设施信息
     */
    @JsonProperty("Amenities")
    private Amenities amenities;
    
    /**
     * 酒店邮箱
     */
    @JsonProperty("email")
    private String email;
    
    /**
     * 酒店网站
     */
    @JsonProperty("website")
    private String website;
    
    /**
     * 酒店电话
     */
    @JsonProperty("Phone")
    private String phone;
    
    /**
     * 经度
     */
    @JsonProperty("longitude")
    private String longitude;
    
    /**
     * 纬度
     */
    @JsonProperty("latitude")
    private String latitude;
    
    /**
     * 酒店评级
     */
    @JsonProperty("HotelRating")
    private String hotelRating;
    
    /**
     * 酒店地址
     */
    @JsonProperty("HotelAddress")
    private String hotelAddress;
    
    /**
     * 酒店图片
     */
    @JsonProperty("HotelImages")
    private List<HotelImage> hotelImages;
    
    /**
     * 房型选择信息
     */
    @JsonProperty("SectionSelection")
    private List<SectionSelection> sectionSelection;

    
    /**
     * 设施信息
     */
    @Data
    public static class Amenities {
        /**
         * 酒店设施
         */
        @JsonProperty("HotelAmenities")
        private List<HotelAmenity> hotelAmenities;
        
        /**
         * 房间设施
         */
        @JsonProperty("RoomAmenities")
        private List<RoomAmenity> roomAmenities;
    }
    
    /**
     * 酒店设施
     */
    @Data
    public static class HotelAmenity {
        /**
         * 设施名称
         */
        @JsonProperty("AmenityName")
        private String amenityName;
    }
    
    /**
     * 房间设施
     */
    @Data
    public static class RoomAmenity {
        /**
         * 房间设施名称
         */
        @JsonProperty("RoomAmenityName")
        private String roomAmenityName;
    }
    
    /**
     * 酒店图片
     */
    @Data
    public static class HotelImage {
        /**
         * 缩略图URL
         */
        @JsonProperty("ThumbnailUrl")
        private String thumbnailUrl;
        
        /**
         * 大图URL
         */
        @JsonProperty("BigUrl")
        private String bigUrl;
    }
    
    /**
     * 房型选择信息
     */
    @Data
    public static class SectionSelection {
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
         * 房间详情
         */
        @JsonProperty("RoomDetails")
        private List<Object> roomDetails;
        
        /**
         * 房型详情列表
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
         * 是否可用
         */
        @JsonProperty("Available")
        private String available;
        
        /**
         * 房间数量
         */
        @JsonProperty("NumberOfRooms")
        private Integer numberOfRooms;
        
        /**
         * 成人数量
         */
        @JsonProperty("NumberOfAdults")
        private Integer numberOfAdults;
        
        /**
         * 儿童数量
         */
        @JsonProperty("NumberOfChild")
        private Integer numberOfChild;
        
        /**
         * 房型价格
         */
        @JsonProperty("RoomRate")
        private BigDecimal roomRate;
        
        /**
         * 房型类型
         */
        @JsonProperty("RoomType")
        private String roomType;
        
        /**
         * 房型分类
         */
        @JsonProperty("RoomCategory")
        private String roomCategory;
        
        /**
         * 餐食基础
         */
        @JsonProperty("MealBasis")
        private String mealBasis;
        
        /**
         * 房类唯一ID
         */
        @JsonProperty("ClassUniqueId")
        private String classUniqueId;
        
        /**
         * 退款政策文本
         */
        @JsonProperty("RefundPolicyText")
        private String refundPolicyText;
        
        /**
         * 价格明细
         */
        @JsonProperty("RateBreakup")
        private List<RateBreakup> rateBreakup;
    }
    
    /**
     * 价格明细
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
         * 每晚显示价格
         */
        @JsonProperty("DisplayNightlyRate")
        private BigDecimal displayNightlyRate;
    }
}
