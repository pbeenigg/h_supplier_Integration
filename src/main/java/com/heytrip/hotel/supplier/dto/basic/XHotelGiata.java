package com.heytrip.hotel.supplier.dto.basic;

import lombok.Data;

/**
 * 兼容 DTO：GIATA 酒店映射返回对象
 */
@Data
public class XHotelGiata {
    private String hotelCode;
    private String giataId;
    private String name;
    private String cityCode;
    private String cityName;
    private String countryCode;
    private String longDesc;
    private Double latitude;
    private Double longitude;
    private Double rating;
    private String address;
    private String mainImage;


}
