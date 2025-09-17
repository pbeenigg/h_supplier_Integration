package com.heytrip.hotel.supplier.dto.basic;

import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * 兼容 DTO：GIATA 酒店映射返回对象
 */
@Data
public class XHotelGiata {

    @Comment("酒店ID")
    private String hotelId;

    @Comment("酒店编码")
    private String hotelCode;

    @Comment("GIATA 编码")
    private String giataId;

    @Comment("酒店名称")
    private String name;

    @Comment("城市代码")
    private String cityCode;

    @Comment("城市名称")
    private String cityName;

    @Comment("国家代码")
    private String countryCode;

    @Comment("描述")
    private String longDesc;

    @Comment("纬度")
    private Double latitude;

    @Comment("经度")
    private Double longitude;

    @Comment("评分/星级")
    private Double rating;

    @Comment("地址")
    private String address;

    @Comment("主图URL")
    private String mainImage;


}
