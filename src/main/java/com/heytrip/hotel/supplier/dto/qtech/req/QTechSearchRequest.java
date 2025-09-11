package com.heytrip.hotel.supplier.dto.qtech.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * QTECH酒店搜索请求DTO
 * 
 * @author Pax
 */
@Data
public class QTechSearchRequest {
    
    /**
     * 接口名称
     */
    private String action = "hotel_search";
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 入住日期 DD/MM/YYYY
     */
    @JsonProperty("checkin_date")
    private String checkinDate;
    
    /**
     * 离店日期 DD/MM/YYYY
     */
    @JsonProperty("checkout_date")
    private String checkoutDate;
    
    /**
     * 目的地国家ID
     */
    @JsonProperty("sel_country")
    private String selCountry;
    
    /**
     * 目的地城市ID
     */
    @JsonProperty("sel_city")
    private String selCity;
    
    /**
     * 星级过滤，逗号分隔
     */
    @JsonProperty("chk_ratings")
    private String chkRatings = "1.0,2.0,3.0,4.0,5.0";
    
    /**
     * 国籍ID
     */
    @JsonProperty("sel_nationality")
    private String selNationality;
    
    /**
     * 居住国ID
     */
    @JsonProperty("country_of_residence")
    private String countryOfResidence;
    
    /**
     * 币种
     */
    @JsonProperty("sel_currency")
    private String selCurrency = "USD";
    
    /**
     * 仅返回有房 1/0
     */
    @JsonProperty("availableonly")
    private Integer availableonly = 1;
    
    /**
     * 房间数
     */
    @JsonProperty("number_of_rooms")
    private Integer numberOfRooms;
    
    /**
     * 房间详情JSON数组
     */
    @JsonProperty("roomDetails")
    private String roomDetails;
    
    /**
     * 酒店名称（可选）
     */
    @JsonProperty("sel_hotel")
    private String selHotel = "";
    
    /**
     * 酒店ID列表，逗号分隔（可选）
     */
    @JsonProperty("hotel_ids")
    private String hotelIds = "";
    
    /**
     * 响应压缩
     */
    private String gzip = "no";
    
    /**
     * 超时秒数
     */
    private Integer timeout = 30;
    
    /**
     * 是否返回静态信息
     */
    @JsonProperty("static_data")
    private Integer staticData = 1;
    
    /**
     * 每酒店房型上限
     */
    @JsonProperty("limit_hotel_room_type")
    private Integer limitHotelRoomType = 5;
    
    /**
     * 房间详情内部类
     */
    @Data
    public static class RoomDetail {
        /**
         * 成人数
         */
        @JsonProperty("numberOfAdults")
        private Integer numberOfAdults;
        
        /**
         * 儿童数
         */
        @JsonProperty("numberOfChild")
        private Integer numberOfChild;
        
        /**
         * 儿童年龄，逗号分隔
         */
        @JsonProperty("ChildAge")
        private String childAge;
    }
}
