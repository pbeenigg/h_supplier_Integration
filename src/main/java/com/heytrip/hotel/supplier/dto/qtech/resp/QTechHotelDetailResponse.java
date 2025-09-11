package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * QTECH酒店详情响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechHotelDetailResponse {
    
    /**
     * 消息
     */
    @JsonProperty("Message")
    private String message;
    
    /**
     * 酒店详情
     */
    @JsonProperty("HotelDetail")
    private HotelDetail hotelDetail;
    
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
     * 酒店详情
     */
    @Data
    public static class HotelDetail {
        /**
         * 酒店ID
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
         * 国家名称
         */
        @JsonProperty("CountryName")
        private String countryName;
        
        /**
         * 城市名称
         */
        @JsonProperty("CityName")
        private String cityName;
        
        /**
         * 酒店描述
         */
        @JsonProperty("HotelDescription")
        private String hotelDescription;
        
        /**
         * 酒店设施
         */
        @JsonProperty("HotelFacilities")
        private List<String> hotelFacilities;
        
        /**
         * 酒店图片
         */
        @JsonProperty("HotelImages")
        private List<HotelImage> hotelImages;
        
        /**
         * 房型列表
         */
        @JsonProperty("RoomTypes")
        private List<RoomType> roomTypes;
    }
    
    /**
     * 酒店图片
     */
    @Data
    public static class HotelImage {
        /**
         * 图片URL
         */
        @JsonProperty("ImageUrl")
        private String imageUrl;
        
        /**
         * 图片描述
         */
        @JsonProperty("ImageDescription")
        private String imageDescription;
    }
    
    /**
     * 房型信息
     */
    @Data
    public static class RoomType {
        /**
         * 房型ID
         */
        @JsonProperty("RoomTypeId")
        private String roomTypeId;
        
        /**
         * 房型名称
         */
        @JsonProperty("RoomTypeName")
        private String roomTypeName;
        
        /**
         * 房型描述
         */
        @JsonProperty("RoomTypeDescription")
        private String roomTypeDescription;
        
        /**
         * 房型设施
         */
        @JsonProperty("RoomFacilities")
        private List<String> roomFacilities;
        
        /**
         * 房型图片
         */
        @JsonProperty("RoomImages")
        private List<HotelImage> roomImages;
    }
}
