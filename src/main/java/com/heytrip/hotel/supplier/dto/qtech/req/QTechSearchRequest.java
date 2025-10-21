package com.heytrip.hotel.supplier.dto.qtech.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heytrip.hotel.supplier.adapter.builder.JsonParam;
import lombok.Data;

import java.util.List;

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
     * 房间详情列表，列表长度与房间数"numberOfRooms"必须保持一致
     */
    @JsonProperty("roomDetails")
    @JsonParam
    private List<RoomDetail> roomDetails;

    /**
     * 酒店名称（可选）
     */
    @JsonProperty("sel_hotel")
    private String selHotel = "";

    /**
     * 酒店ID列表，多个用逗号分隔（可选）（最多传递 100个）
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
    private Integer timeout = 60;

    /**
     * 是否返回静态信息
     */
    @JsonProperty("static_data")
    private Integer staticData = 1;

    /**
     * 每酒店房型上限
     * 默认5，返回 5 个最优房型
     * 不传 返回所有可用的房型
     */
    //@JsonProperty("limit_hotel_room_type")
    //private Integer limitHotelRoomType = 5;

    /**
     * 校验房间详情列表与房间数的一致性
     *
     * @throws IllegalArgumentException 当房间详情列表长度与房间数不一致时抛出异常
     */
    public void validateRoomDetails() {
        if (numberOfRooms == null) {
            throw new IllegalArgumentException("房间数(numberOfRooms)不能为空");
        }

        if (numberOfRooms <= 0) {
            throw new IllegalArgumentException("房间数(numberOfRooms)必须大于0，当前值: " + numberOfRooms);
        }

        if (roomDetails == null) {
            throw new IllegalArgumentException("房间详情列表(roomDetails)不能为空");
        }

        if (roomDetails.size() != numberOfRooms) {
            throw new IllegalArgumentException(
                    String.format("房间详情列表长度(%d)与房间数(%d)不一致，必须保持相等",
                            roomDetails.size(), numberOfRooms));
        }

        // 校验每个房间详情的有效性
        for (int i = 0; i < roomDetails.size(); i++) {
            RoomDetail room = roomDetails.get(i);
            if (room == null) {
                throw new IllegalArgumentException("第" + (i + 1) + "个房间详情不能为空");
            }
            room.validate(i + 1);
        }
    }

    /**
     * 检查房间详情列表与房间数是否一致
     *
     * @return true表示一致，false表示不一致
     */
    public boolean isRoomDetailsValid() {
        try {
            validateRoomDetails();
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 获取房间详情校验错误信息
     *
     * @return 校验错误信息，如果校验通过则返回null
     */
    public String getRoomDetailsValidationError() {
        try {
            validateRoomDetails();
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    /**
     * 房间详情内部类
     */
    @Data
    public static class RoomDetail {
        /**
         * 成人数（必填）
         */
        @JsonProperty("numberOfAdults")
        private Integer numberOfAdults;

        /**
         * 儿童数 （可选）
         */
        @JsonProperty("numberOfChild")
        private Integer numberOfChild;

        /**
         * 儿童年龄,多个儿童用逗号分割 （填写儿童数必填年龄）
         */
        @JsonProperty("ChildAge")
        private String childAge;

        /**
         * 校验房间详情的有效性
         *
         * @param roomIndex 房间索引（从1开始），用于错误信息提示
         * @throws IllegalArgumentException 当房间详情无效时抛出异常
         */
        public void validate(int roomIndex) {
            if (numberOfAdults == null || numberOfAdults <= 0) {
                throw new IllegalArgumentException(
                        String.format("第%d个房间的成人数(numberOfAdults)必须大于0，当前值: %s",
                                roomIndex, numberOfAdults));
            }

            if (numberOfAdults > 10) { // 最大成人数为10
                throw new IllegalArgumentException(
                        String.format("第%d个房间的成人数(numberOfAdults)不能超过10，当前值: %d",
                                roomIndex, numberOfAdults));
            }


            if (numberOfChild!= null && numberOfChild > 3) { // 限制每个房间儿童数为3
                throw new IllegalArgumentException(
                        String.format("第%d个房间的儿童数(numberOfChild)不能超过3，当前值: %d",
                                roomIndex, numberOfChild));
            }

            // 如果有儿童，检查儿童年龄是否提供
            if (numberOfChild != null && numberOfChild > 0) {
                if (childAge == null || childAge.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("第%d个房间有%d个儿童，但未提供儿童年龄(childAge)",
                                    roomIndex, numberOfChild));
                }

                // 校验儿童年龄格式（逗号分隔的数字）
                String[] ages = childAge.split(",");
                if (ages.length != numberOfChild) {
                    throw new IllegalArgumentException(
                            String.format("第%d个房间的儿童年龄数量(%d)与儿童数(%d)不匹配",
                                    roomIndex, ages.length, numberOfChild));
                }

                // 检查每个年龄是否为有效数字
                for (int i = 0; i < ages.length; i++) {
                    try {
                        int age = Integer.parseInt(ages[i].trim());
                        if (age < 0 || age > 12) { // 假设儿童年龄范围0-12
                            throw new IllegalArgumentException(
                                    String.format("第%d个房间的第%d个儿童年龄(%d)无效，年龄应在0-12之间",
                                            roomIndex, i + 1, age));
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                String.format("第%d个房间的第%d个儿童年龄格式无效: %s",
                                        roomIndex, i + 1, ages[i].trim()));
                    }
                }
            } else {
                // 如果没有儿童，儿童年龄应该为空
                if (childAge != null && !childAge.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("第%d个房间没有儿童，但提供了儿童年龄: %s",
                                    roomIndex, childAge));
                }
            }
        }
    }
}
