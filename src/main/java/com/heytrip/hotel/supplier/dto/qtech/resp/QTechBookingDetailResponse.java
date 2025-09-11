package com.heytrip.hotel.supplier.dto.qtech.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * QTECH预订详情响应DTO
 * 
 * @author Pax
 */
@Data
public class QTechBookingDetailResponse extends QTechBaseResponse {

    
    /**
     * 预订详情
     */
    @JsonProperty("BookingDetail")
    private BookingDetail bookingDetail;

    
    /**
     * 预订详情
     */
    @Data
    public static class BookingDetail {
        /**
         * 预订ID
         */
        @JsonProperty("Id")
        private String id;
        
        /**
         * 代理ID
         */
        @JsonProperty("AgentId")
        private String agentId;
        
        /**
         * 预订参考号
         */
        @JsonProperty("BookingReference")
        private String bookingReference;
        
        /**
         * 预订日期
         */
        @JsonProperty("BookingDate")
        private String bookingDate;
        
        /**
         * 总费用
         */
        @JsonProperty("TotalCharges")
        private BigDecimal totalCharges;
        
        /**
         * 主客人称谓
         */
        @JsonProperty("LeaderTitle")
        private String leaderTitle;
        
        /**
         * 主客人名
         */
        @JsonProperty("LeaderFirstName")
        private String leaderFirstName;
        
        /**
         * 主客人姓
         */
        @JsonProperty("LeaderLastName")
        private String leaderLastName;
        
        /**
         * 币种
         */
        @JsonProperty("CurrencyCode")
        private String currencyCode;
        
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
         * 国家名称
         */
        @JsonProperty("CountryName")
        private String countryName;
        
        /**
         * 城市ID
         */
        @JsonProperty("CityId")
        private String cityId;
        
        /**
         * 酒店地址
         */
        @JsonProperty("HotelAddress1")
        private String hotelAddress1;
        
        /**
         * 当前状态
         */
        @JsonProperty("CurrentStatus")
        private String currentStatus;
        
        /**
         * 总成人数
         */
        @JsonProperty("TotalAdults")
        private String totalAdults;
        
        /**
         * 总儿童数
         */
        @JsonProperty("TotalChildren")
        private String totalChildren;
        
        /**
         * 总房间数
         */
        @JsonProperty("TotalRooms")
        private String totalRooms;
        
        /**
         * 入住日期
         */
        @JsonProperty("CheckInDate")
        private String checkInDate;
        
        /**
         * 离店日期
         */
        @JsonProperty("CheckOutDate")
        private String checkOutDate;
        
        /**
         * 代理价格
         */
        @JsonProperty("AgentRate")
        private String agentRate;
        
        /**
         * 代理参考号
         */
        @JsonProperty("AgentRefNo")
        private String agentRefNo;
        
        /**
         * 房间详情
         */
        @JsonProperty("RoomDetail")
        private List<RoomDetail> roomDetail;
        
        /**
         * 合同备注
         */
        @JsonProperty("CommentContract")
        private String commentContract;
        
        /**
         * 取消规则
         */
        @JsonProperty("CancellationPolicy")
        private String cancellationPolicy;
        
        /**
         * 是否可取消
         */
        @JsonProperty("IsCancellable")
        private String isCancellable;
        
        /**
         * 取消费用
         */
        @JsonProperty("CancellationCharges")
        private BigDecimal cancellationCharges;
    }
    
    /**
     * 房间详情
     */
    @Data
    public static class RoomDetail {
        /**
         * 房型描述
         */
        @JsonProperty("RoomTypeDescription")
        private String roomTypeDescription;
        
        /**
         * 房间数
         */
        @JsonProperty("NumberOfRoom")
        private String numberOfRoom;
        
        /**
         * 成人数
         */
        @JsonProperty("NumberOfAdults")
        private String numberOfAdults;
        
        /**
         * 儿童数
         */
        @JsonProperty("NumberOfChildren")
        private String numberOfChildren;
        
        /**
         * 餐型
         */
        @JsonProperty("MealBasis")
        private String mealBasis;
        
        /**
         * 乘客列表
         */
        @JsonProperty("Passengers")
        private List<Passenger> passengers;
    }
    
    /**
     * 乘客信息
     */
    @Data
    public static class Passenger {
        /**
         * 称谓
         */
        @JsonProperty("Salutation")
        private String salutation;
        
        /**
         * 名
         */
        @JsonProperty("FirstName")
        private String firstName;
        
        /**
         * 姓
         */
        @JsonProperty("LastName")
        private String lastName;
        
        /**
         * 乘客类型
         */
        @JsonProperty("PassengerType")
        private String passengerType;
        
        /**
         * 年龄
         */
        @JsonProperty("Age")
        private String age;
    }
}
