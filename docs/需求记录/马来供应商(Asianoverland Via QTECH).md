# QTECH API 供应商对接文档（Asianoverland）
日期：2025-09-04

本文档用于指导对接 QTECH 技术通道（供应商标识：Asianoverland）的酒店业务流程与接口规范，包含环境信息、调用顺序、接口参数、示例、静态数据、状态说明及对接注意事项。

- 供应商技术通道官网：https://www.qtechsoftware.com/
- 联系人：Godrey Pereira
- 联系邮箱：技术支持 <godrey.pereira@qtechsoftware.com>，客户经理 <halancia.deva@qtechsoftware.com>
- 联系电话：+91.22.46050602
- 类型：国际供应商
- 供应商类型：技术通道（不直接提供技术团队，通过通道分发资源）
- 供应商标识：Asianoverland

---

## 目录
1. 使用限制与规则
2. 环境与认证
3. 接口总览与调用流程
4. 接口详情
    - 4.1 酒店搜索 hotel_search
    - 4.2 酒店详情 hotel_detail
    - 4.3 取消政策 hotel_cancellation_policy
    - 4.4 酒店预订 hotel_reservation
    - 4.5 预订详情 booking_detail
    - 4.6 取消费用 get_cancellation_charges
    - 4.7 取消预订 cancel_the_booking
5. 预订状态枚举
6. 静态数据与 FTP 访问
7. 对接过程与注意事项
8. 术语约定与参数统一

---

## 1. 使用限制与规则
- 每笔预订的夜数：最多 30 晚
- 每笔预订的客人上限：5 间房、10 位客人
- 儿童年龄：0-12 岁
- 每间房最多儿童数：3 名
- 服务日期不应超过未来 365 天

---

## 2. 环境与认证

- 测试账号
    - 用户名：Heytrip_Test
    - 密码：Welcome@@123

- API 基础地址
    - 搜索（hotel_search）使用：`http://colosseum.otrams.com:8087/ws/index.php`
    - 其他（detail/cancellation/reservation/booking...）：`https://colosseum.otrams.com/ws/index.php`

- 通用请求参数
    - action：接口名称（如 hotel_search / hotel_detail / ...）
    - username：账号（测试：Heytrip_Test）
    - password：密码（测试：Welcome@@123）

- 超时建议
    - 预订步骤建议超时：180 秒
    - 若 180 秒内无响应，请立即使用 booking_detail 通过 agent_ref_no 轮询查询状态

---

## 3. 接口总览与调用流程

标准流程（建议遵循）：
1) hotel_search：检索目的地酒店与可订房型（必选）
2) hotel_detail：拉取指定酒店详细信息（可选）
3) hotel_cancellation_policy：获取所选房型的取消条款（必选）
4) hotel_reservation：提交预订（必选）
5) booking_detail：查询预订详情（可选，用于状态轮询与兜底）
6) get_cancellation_charges：获取当前时点的取消费用（必选）
7) cancel_the_booking：执行取消（必选）

注意：
- hotel_detail 与 hotel_cancellation_policy 的 unique_id/section_unique_id 来自 search/detail 响应。
- 预订时 expected_price 必须与搜索结果一致，否则会报错。
- agent_ref_no 必须对每单唯一，用于后续查询与幂等兜底。

---

## 4. 接口详情

### 4.1 酒店搜索 hotel_search
- 请求地址：`http://colosseum.otrams.com:8087/ws/index.php`
- 示例：
```
GET /ws/index.php?action=hotel_search
  &username=Heytrip_Test
  &password=Welcome@@123
  &checkin_date=22/04/2020
  &checkout_date=23/04/2020
  &sel_country=138
  &sel_city=71649
  &chk_ratings=1.0,2.0,3.0,4.0,5.0
  &sel_nationality=106
  &country_of_residence=106
  &sel_currency=INR
  &availableonly=1
  &number_of_rooms=1
  &roomDetails=[{"numberOfAdults":1}]
  &sel_hotel=
  &gzip=no
  &timeout=30
  &static_data=1
  &limit_hotel_room_type=5
```

- 参数说明：
    - checkin_date / checkout_date：DD/MM/YYYY（例：22/04/2020）
    - sel_country / sel_city：目的地国家/城市 ID（例：138 / 71649）
    - chk_ratings：星级过滤，逗号分隔（例：1.0,2.0,...）
    - sel_nationality / country_of_residence：国籍/居住国 ID（例：106）
    - sel_currency：币种（例：INR）
    - availableonly：仅返回有房（1/0）
    - gzip：响应压缩（yes/no）
    - static_data：是否返回静态信息（1/0）
    - limit_hotel_room_type：每酒店房型上限（例：5）
    - timeout：超时秒（例：30）
    - sel_hotel：酒店名称（可选）
    - hotel_ids：酒店 ID 列表，逗号分隔（可选）
    - number_of_rooms：房间数（例：1）
    - roomDetails：JSON 数组，长度与房间数一致
        - numberOfAdults：成人数
        - numberOfChild：儿童数（可选）
        - ChildAge：儿童年龄，逗号分隔（可选）

- 响应示例（精简）：
```json
{
  "TotalCount": 1,
  "WebServiceVersion": "2.0",
  "Message": "success",
  "HotelList": [
    {
      "HotelId": "6b517e6a59eba07e3d1580082ff2bdcd876d899608a695ee0ea45a5448a1b462",
      "HotelName": "CASSELLS AL BARSHA HOTEL",
      "LocalHotelId": "OT000228787",
      "PropertyRating": "4.0",
      "Available": "1",
      "Latitude": 25.113475,
      "Longitude": 55.191621,
      "Address": "Sheikh Zayed Road PO Box 114400",
      "RateCurrencyCode": "INR",
      "TotalCharges": "3856.6",
      "HotelProperty": [
        {
          "DisplayRoomRate": 3856.6,
          "Type": "Selection",
          "SectionUniqueId": "25_181195-2_1_93642",
          "RoomRates": [
            {
              "Available": 1,
              "NumberOfRooms": 1,
              "NumberOfAdults": "1",
              "NumberOfChild": "0",
              "RoomRate": 3856.6,
              "RoomType": "Single-Standard Room Twin Bed",
              "RoomCategory": "Standard Room Twin Bed",
              "MealBasis": "Room Only",
              "ClassUniqueId": "181195_1_89010",
              "RateBreakup": [
                {
                  "Date": "22-04-2020",
                  "Day": "Wednesday",
                  "DisplayNightlyRate": 3856.6
                }
              ]
            }
          ]
        }
      ]
    }
  ],
  "SearchUniqueId": "870420200206023349484798224",
  "StartTime": "2020-02-04 06:02:33",
  "EndTime": "2020-02-04 06:02:53"
}
```

- 字段说明要点：
    - SearchUniqueId：本次搜索会话唯一标识，后续 detail/cancellation/reservation 需传
    - SectionUniqueId：房型组合唯一标识（后续取消政策与预订使用）
    - ClassUniqueId：具体房型/房类唯一ID（预订 roomDetails.roomClassId 使用）
    - TotalCharges/DisplayRoomRate：房型总价（注意与 expected_price 校验一致）
    - RateBreakup：逐夜价格明细

- 响应字段表格：  
  - 基础响应  

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | TotalCount | int | 1 | 返回酒店数量 |
    | WebServiceVersion | string | "2.0" | API 版本 |
    | Message | string | "success" | 请求结果（success/failed） |
    | SearchUniqueId | string | "870420200206023349484798224" | 本次搜索会话ID，detail/cancellation/reservation需传 |
    | StartTime | string | "2020-02-04 06:02:33" | 开始时间 |
    | EndTime | string | "2020-02-04 06:02:53" | 结束时间 |

  - HotelList[n]

      | 字段名 | 类型 | 示例 | 备注 |
        | --- | --- | --- | --- |
      | HotelId | string | "6b51..." | 运行时酒店ID |
      | LocalHotelId | string | "OT000228787" | 静态酒店ID（可做映射） |
      | HotelName | string | "CASSELLS AL BARSHA HOTEL" | 酒店名 |
      | PropertyRating | string | "4.0" | 星级 |
      | Available | string/int | "1" | 1可订/0请求单 |
      | Latitude | float | 25.113475 | 纬度 |
      | Longitude | float | 55.191621 | 经度 |
      | Address | string | "Sheikh Zayed Road ..." | 地址 |
      | RateCurrencyCode | string | "INR" | 币种 |
      | TotalCharges | string/float | "3856.6" | 最便宜房型总价 |

  - HotelList[n].HotelProperty[m]
    
      | 字段名 | 类型 | 示例 | 备注 |
        | --- | --- | --- | --- |
      | DisplayRoomRate | float | 3856.6 | 房型组合总价 |
      | Type | string | "Selection" | 内部使用 |
      | SectionUniqueId | string | "25_181195-2_1_93642" | 房型组合唯一ID（后续取消政策/预订必传） |

  - HotelList[n].HotelProperty[m].RoomRates[k]

      | 字段名 | 类型 | 示例 | 备注 |
        | --- | --- | --- | --- |
      | Available | int | 1 | 是否可订 |
      | NumberOfRooms | int | 1 | 房间数 |
      | NumberOfAdults | string | "1" | 成人数 |
      | NumberOfChild | string | "0" | 儿童数 |
      | RoomRate | float | 3856.6 | 价格 |
      | RoomType | string | "Single-Standard..." | 房型描述 |
      | RoomCategory | string | "Standard..." | 房型类别 |
      | MealBasis | string | "Room Only" | 餐型 |
      | ClassUniqueId | string | "181195_1_89010" | 具体房类ID（预订roomDetails.roomClassId使用） |
      | RateBreakup[] | array | [{"Date","Day","DisplayNightlyRate"}] | 日价明细 |

---

### 4.2 酒店详情 hotel_detail
- 请求地址：`https://colosseum.otrams.com/ws/index.php`
- 示例：
```
GET /ws/index.php?action=hotel_detail
  &username=Heytrip_Test
  &password=Welcome@@123
  &hotel_id=OT000016097
  &unique_id=824-010-20250909112150-010-981795-010-175741691082371
```
- 参数说明：
    - hotel_id：酒店 ID（例：OT000016097）
    - unique_id：来自 hotel_search 的 unique_id

- 响应示例（精简）：
```json
{
  "Message": "success",
  "HotelId": "6b517e6a59eba07e3d1580082ff2bdcd876d899608a695ee0ea45a5448a1b462",
  "HotelName": "CASSELLS AL BARSHA HOTEL",
  "Description": "....",
  "Amenities": {
    "HotelAmenities": [{"AmenityName": "Car Parking"}],
    "RoomAmenities": [{"RoomAmenityName": "Hair Dryer"}]
  },
  "longitude": 55.191621,
  "latitude": 25.113475,
  "HotelRating": "4.0",
  "HotelAddress": "Sheikh Zayed Road PO Box 114400",
  "HotelImages": [
    {"ThumbnailUrl": "http://URL/...","BigUrl": "http://URL/..."}
  ],
  "SectionSelection": [
    {
      "DisplayRoomRate": 3856.6,
      "Type": "Selection",
      "SectionUniqueId": "25_181195-2_1_93642",
      "RoomRates": [
        {
          "Available": "1",
          "NumberOfRooms": 1,
          "NumberOfAdults": "1",
          "NumberOfChild": "0",
          "RoomRate": 3856.6,
          "RoomType": "Single-Standard Room Twin Bed",
          "RoomCategory": "Standard Room Twin Bed",
          "MealBasis": "Room Only",
          "ClassUniqueId": "181195_1_89010"
        }
      ]
    }
  ],
  "StartTime": "2020-02-04 06:03:50",
  "EndTime": "2020-02-04 06:04:01"
}
```

- 字段说明要点：
    - Amenities.HotelAmenities/RoomAmenities：酒店/房间设施列表
    - SectionSelection：与搜索结果一致的房型集合（含 SectionUniqueId、ClassUniqueId）

- 响应字段表格：
  - 顶层

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Message | string | "success" | 成功/失败 |
    | HotelId | string | "6b51..." | 运行时酒店ID |
    | HotelName | string | "CASSELLS AL BARSHA HOTEL" | 酒店名 |
    | Description | string | "..." | 描述/须知 |
    | longitude | float | 55.191621 | 经度 |
    | latitude | float | 25.113475 | 纬度 |
    | HotelRating | string | "4.0" | 星级 |
    | HotelAddress | string | "..." | 地址 |
    | StartTime/EndTime | string | "2020-02-04 06:03:50" | 调用时间 |

  - Amenities

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | HotelAmenities[].AmenityName | string | "Car Parking" | 酒店设施 |
    | RoomAmenities[].RoomAmenityName | string | "Hair Dryer" | 房内设施 |

  - HotelImages[]  

    | 字段名 | 类型 | 示例 | 备注 |
    | --- | --- | --- | --- |
    | ThumbnailUrl | string | "http://..." | 小图URL |
    | BigUrl | string | "http://..." | 大图URL |

  - SectionSelection[]

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | DisplayRoomRate | float | 3856.6 | 组合总价 |
    | Type | string | "Selection" | 内部使用 |
    | SectionUniqueId | string | "25_..." | 组合ID（后续取消政策/预订用） |
    | RoomRates[].ClassUniqueId | string | "181195_..." | 房类ID（预订用） |
    | RoomRates[].Available | string/int | "1" | 是否可订 |
    | RoomRates[].RoomType | string | "Single-..." | 房型描述 |
    | RoomRates[].MealBasis | string | "Room Only" | 餐型 |
---

### 4.3 取消政策 hotel_cancellation_policy
- 请求地址：`https://colosseum.otrams.com/ws/index.php`
- 示例（URL 编码项已省略展示）：
```
GET /ws/index.php?action=hotel_cancellation_policy
  &username=Heytrip_Test
  &password=Welcome@@123
  &hotel_id=OT000016097
  &unique_id=824-010-20250909112150-010-981795-010-1757416910823714670-010-
  &section_unique_id=ZG90dzoxXzMwOTU0X2RvdWJsZXBsdXNjaGlsZF8xXzJfMV81X2RlbHV4ZV9yb29tb25seV90cnVlXzQyRF84Ni4wMCVDUDpxcXE6Z28%3D
  &gzip=no
```
- 参数说明：
    - hotel_id：酒店 ID
    - unique_id：来自 search/detail 响应
    - section_unique_id：房型唯一 ID，来自 search/detail 响应
    - gzip：yes/no

- 响应示例（精简）：
```json
{
  "Message": "success",
  "CancellationCurrency": "INR",
  "TotalBookingAmount": 3856.6,
  "ContractComment": "Hotel Remark: ... additional fees ...",
  "CancellationHours": 1917,
  "AppliedAgentCharges": 3856.6,
  "BookingAllowedInfo": {
    "Message": "success",
    "Status": "success",
    "SoldOut": "No",
    "MessageInfo": "You are making a booking within cancellation policy.",
    "BookingAllowed": "yes"
  },
  "StartTime": "2020-02-04 06:05:52",
  "EndTime": "2020-02-04 06:06:07"
}
```

- 字段说明要点：
    - CancellationHours：免费取消截止的小时数（据此计算 deadline）
    - AppliedAgentCharges：过期后的取消罚金金额
    - BookingAllowedInfo.BookingAllowed：是否允许继续下单

- 响应字段表格：
  - 顶层

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Message | string | "success" | 成功/失败 |
    | CancellationCurrency | string | "INR" | 取消费用币种 |
    | TotalBookingAmount | float | 3856.6 | 预订总额（预订 expected_price 必须相等） |
    | ContractComment | string | "Hotel Remark: ..." | 备注/附加费/政策 |
    | CancellationHours | int | 1917 | 免费取消时间（小时） |
    | AppliedAgentCharges | float | 3856.6 | 逾期取消罚金 |
    | StartTime/EndTime | string | "2020-02-04 06:05:52" | 调用时间 |

  - BookingAllowedInfo

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Status | string | "success" | 状态 |
    | SoldOut | string | "No" | 是否售罄 |
    | MessageInfo | string | "You are making a booking within cancellation policy." | 说明 |
    | BookingAllowed | string | "yes" | 是否允许下单 |
---

### 4.4 酒店预订 hotel_reservation
- 请求地址：`https://colosseum.otrams.com/ws/index.php`
- 规则说明（关键要求）：
    - 所有房间详情和所有客人详情必须在请求中提供。
    - 儿童乘客称谓统一为 “Children”；如含儿童，需在 roomDetails 中提供年龄（age）。
    - agent_ref_no 必须每单唯一。
    - 使用最近的取消政策且与所订房间一致，否则会报错。
    - 预订超时建议 180 秒；若超时未回包，立刻用 booking_detail 携带 agent_ref_no 轮询状态。

- 示例（示意）：
```url
GET /ws/index.php?action=hotel_reservation
  &username=Heytrip_Test
  &password=Welcome@@123
  &hotel_id=OT000016097
  &unique_id=824-010-20250910024521-010-981963-010-1757472321415648652-010-
  &section_unique_id=MzA5NTRfMF9kb3VibGVwbHVzY2hpbGRfMV8yXzFfNV9kZWx1eGVyb29tX2JyZWFrZmFzdF90cnVl
  &agent_ref_no=824-010-20250910024521-010-981963-010-1757472321415648652-010-
  &roomDetails=[{"numberOfChilds":"1","roomClassId":"...","passangers":[{"salutation":"Mr","first_name":"Bhargavi","last_name":"test"}]}]
  &expected_price=195.83
```

```
- 字段说明：
    - roomClassId：等同 ClassUniqueId（来自搜索/详情的 RoomRates.ClassUniqueId）
    - passangers：包含该房间全部旅客（成人+儿童），儿童需含 age
    - expected_price：需与 TotalBookingAmount 一致（来自取消政策响应）

- 响应示例（精简）：
```json
{
  "Status": "success",
  "Message": "Success",
  "BookingServiceType": "hotels",
  "BookingDetail": {
    "Id": "2746",
    "AgentId": "87",
    "BookingReference": "ABC2802746",
    "BookingDate": "2020-02-04 06:07:56",
    "TotalCharges": 3856.6,
    "LeaderTitle": "Mr",
    "LeaderFirstName": "Bhargavi",
    "LeaderLastName": "test",
    "CurrencyCode": "INR",
    "LocalHotelId": "OT000228787",
    "HotelName": "CASSELLS AL BARSHA HOTEL",
    "CountryName": "United Arab Emirates",
    "CityId": "Dubai",
    "HotelAddress1": "Sheikh Zayed Road PO Box 114400",
    "CurrentStatus": "vouchered",
    "TotalAdults": "1",
    "TotalChildren": "0",
    "TotalRooms": "1",
    "CheckInDate": "2020-04-22",
    "CheckOutDate": "2020-04-23",
    "AgentRate": "3856.600",
    "AgentRefNo": "5874",
    "RoomDetail": [
      {
        "RoomTypeDescription": "Single-Standard Room Twin Bed",
        "NumberOfRoom": "1",
        "Passengers": [
          {
            "Salutation": "Mr",
            "FirstName": "Bhargavi",
            "LastName": "test",
            "PassengerType": "adult",
            "Age": ""
          }
        ]
      }
    ],
    "CommentContract": "Hotel Remark: ... "
  },
  "StartTime": "2020-02-04 06:07:54",
  "EndTime": "2020-02-04 06:07:58"
}
```

- 字段说明要点：
    - BookingDetail.CurrentStatus：订单状态（vouchered/on_request/failed/...）
    - AgentRefNo：与请求一致，用于后续 booking_detail 兜底轮询

- 响应字段表格：
  - 顶层

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Status | string | "success" | 成功/失败 |
    | Message | string | "Success" | 文本说明 |
    | BookingServiceType | string | "hotels" | 业务类型 |
    | StartTime/EndTime | string | "2020-02-04 06:07:54" | 调用时间 |

  - BookingDetail

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Id | string | "2746" | 预订ID（后续查询/取消使用） |
    | BookingReference | string | "ABC2802746" | 预订参考号 |
    | BookingDate | string | "2020-02-04 06:07:56" | 下单时间 |
    | TotalCharges | float | 3856.6 | 总金额 |
    | CurrencyCode | string | "INR" | 币种 |
    | LocalHotelId | string | "OT000228787" | 静态酒店ID |
    | HotelName | string | "..." | 酒店名 |
    | CountryName/CityId | string | "United Arab Emirates"/"Dubai" | 国家/城市 |
    | HotelAddress1 | string | "..." | 地址 |
    | CurrentStatus | string | "vouchered" | 订单状态 |
    | TotalAdults/TotalChildren/TotalRooms | string | "1"/"0"/"1" | 人数/房间数 |
    | CheckInDate/CheckOutDate | string | "2020-04-22"/"2020-04-23" | 入住/离店 |
    | AgentRate/GrossAmount | string | "3856.600"/"3856.6" | 金额 |
    | AgentRefNo | string | "5874" | 代理参考号（与请求一致） |
    | CommentContract | string | "Hotel Remark: ..." | 备注/附加费/政策 |

  - BookingDetail.RoomDetail[]

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | RoomTypeDescription | string | "Single-Standard..." | 房型描述 |
    | NumberOfRoom | string | "1" | 房间数 |
    | Passengers[] | array | [{"Salutation":"Mr","FirstName":"..."}] | 乘客列表（儿童需含Age） |

  -  BookingDetail.RoomDetail[].Passengers
    
    | 字段名 | 类型 | 示例 | 备注 |
        | --- | --- | --- | --- |
    | Salutation | string | "Mr" | 称谓 |
    | FirstName | string | "Bhargavi" | 名 |
    | LastName | string | "test" | 姓 |
    | PassengerType | string | "adult"/"children" | 乘客类型 |
    | Age | string/int | ""/"5" | 年龄（儿童必填） |
---

### 4.5 预订详情 booking_detail
- 请求地址：`https://colosseum.otrams.com/ws/index.php`
- 用途：查询指定 agent_ref_no（或预订号）的最新状态，作为预订兜底和轮询。
- 示例：
```
GET /ws/index.php?action=booking_detail
  &username=Heytrip_Test
  &password=Welcome@@123
  &agent_ref_no=824-010-20250910041011-010-981964-010-1757477411256884369-010-
  &booking_id=
  &booking_reference=
```
- 参数说明：
    - agent_ref_no：预订提交时的唯一参考号（优先使用）
    - booking_id / booking_reference：来自 reservation 响应（可为空时以 agent_ref_no 查询）

- 响应示例（精简）：
```json
{
  "Message": "Success",
  "Status": "success",
  "BookingServiceType": "hotels",
  "BookingDetail": {
    "Id": "2746",
    "BookingReference": "FUR2802746",
    "LeaderTitle": "Mr",
    "LeaderFirstName": "Bhargavi",
    "LeaderLastName": "test",
    "CurrencyCode": "INR",
    "LocalHotelId": "OT000228787",
    "HotelName": "CASSELLS AL BARSHA HOTEL",
    "CountryName": "United Arab Emirates",
    "CityId": "Dubai",
    "HotelAddress1": "Sheikh Zayed Road PO Box 114400",
    "CurrentStatus": "vouchered",
    "CheckInDate": "2020-04-22",
    "CheckOutDate": "2020-04-23",
    "AgentRate": "3856.600",
    "AgentRefNo": "5874",
    "CancellationPolicy": "If you cancel after ...",
    "RoomDetail": [
      {
        "RoomTypeDescription": "Single-Standard Room Twin Bed",
        "NumberOfRoom": "1",
        "NumberOfAdults": "1",
        "NumberOfChild": "0",
        "Passengers": [
          {"Salutation": "Mr", "FirstName": "Bhargavi", "LastName": "test", "PassengerType": "adult", "Age": ""}
        ]
      }
    ]
  },
  "StartTime": "2020-02-04 06:14:33",
  "EndTime": "2020-02-04 06:14:33"
}
```

- 字段说明要点：
    - CurrentStatus：订单当前状态
    - CancellationPolicy：当前订单取消条款文本（注意与取消费用 API 联动）

- 响应字段表格：
  - 顶层

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Message | string | "Success" | 成功/失败 |
    | Status | string | "success" | 成功/失败 |
    | BookingServiceType | string | "hotels" | 业务类型 |
    | StartTime/EndTime | string | "2020-02-04 06:14:33" | 调用时间 |

  - BookingDetail

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Id | string | "2746" | 预订ID |
    | BookingReference | string | "FUR2802746" | 预订参考号 |
    | CurrentStatus | string | "vouchered" | 订单状态 |
    | CurrencyCode | string | "INR" | 币种 |
    | LocalHotelId | string | "OT000228787" | 静态酒店ID |
    | HotelName | string | "..." | 酒店名 |
    | CountryName/CityId | string | "United Arab Emirates"/"Dubai" | 国家/城市 |
    | HotelAddress1 | string | "..." | 地址 |
    | CheckInDate/CheckOutDate | string | "2020-04-22"/"2020-04-23" | 入住/离店 |
    | AgentRate | string | "3856.600" | 金额 |
    | AgentRefNo | string | "5874" | 代理参考号 |
    | CancellationPolicy | string | "If you cancel..." | 取消条款文本 |

  - BookingDetail.RoomDetail[]

    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | RoomTypeDescription | string | "Single-Standard..." | 房型描述 |
    | NumberOfRoom | string | "1" | 房间数 |
    | NumberOfAdults/NumberOfChild | string | "1"/"0" | 人数 |
    | Passengers[] | array | [{"Salutation":"Mr","FirstName":"..."}] | 乘客列表 |
  
  -  BookingDetail.RoomDetail[].Passengers
  
    | 字段名 | 类型 | 示例 | 备注 |
      | --- | --- | --- | --- |
    | Salutation | string | "Mr" | 称谓 |
    | FirstName | string | "Bhargavi" | 名 |
    | LastName | string | "test" | 姓 |
    | PassengerType | string | "adult"/"children" | 乘客类型 |
    | Age | string/int | ""/"5" | 年龄（儿童必填） |
---

### 4.6 取消费用 get_cancellation_charges
- 请求地址：`https://colosseum.otrams.com/ws/index.php`
- 示例：
```
GET /ws/index.php?action=get_cancellation_charges
  &username=Heytrip_Test
  &password=Welcome@@123
  &booking_id=2312
  &booking_reference=
```
- 参数说明：
    - booking_id：预订 ID（示例：2312）
    - booking_reference：预订参考号（可选）

- 响应示例（精简）：
```json
{
  "Message": "success",
  "Status": "success",
  "AllowCancel": "yes",
  "MessageInfo": "You can cancel this booking.",
  "CancellationCharge": "0",
  "DisplayCurrencyCode": "INR",
  "StartTime": "2020-02-04 06:15:22",
  "EndTime": "2020-02-04 06:15:23"
}
```

- 字段说明要点：
    - AllowCancel：是否允许取消（yes/no）
    - CancellationCharge：此刻取消的费用（配合币种）

- 响应字段表格：

  | 字段名 | 类型 | 示例 | 备注 |
    | --- | --- | --- | --- |
  | Message | string | "success" | 成功/失败 |
  | Status | string | "success" | 成功/失败 |
  | AllowCancel | string | "yes" | 是否允许取消（yes/no） |
  | MessageInfo | string | "You can cancel this booking." | 说明 |
  | CancellationCharge | float/string | "0" | 当前取消费用 |
  | DisplayCurrencyCode | string | "INR" | 币种 |
  | StartTime/EndTime | string | "2020-02-04 06:15:22" | 调用时间 |
---

### 4.7 取消预订 cancel_the_booking
- 请求地址：`https://colosseum.otrams.com/ws/index.php`
- 示例：
```
GET /ws/index.php?action=cancel_the_booking
  &username=Heytrip_Test
  &password=Welcome@@123
  &booking_id=2312
  &booking_reference=
```
- 参数说明：同上

- 响应示例（精简）：
```json
{
  "Message": "success",
  "Status": "success",
  "MessageInfo": "Booking cancelled successfully.",
  "StartTime": "2020-02-01 01:17:49",
  "EndTime": "2020-02-01 01:17:58"
}
```

- 字段说明要点：
    - MessageInfo：取消结果描述（以此为准判断是否取消成功）

- 响应字段表格：

  | 字段名 | 类型 | 示例 | 备注 |
    | --- | --- | --- | --- |
  | Message | string | "success" | 成功/失败 |
  | Status | string | "success" | 成功/失败 |
  | MessageInfo | string | "Booking cancelled successfully." | 取消结果说明 |
  | StartTime/EndTime | string | "2020-02-01 01:17:49"/"2020-02-01 01:17:58" | 调用时间 |
---

## 5. 预订状态枚举

- vouchered：预订已在系统中正确发布并确认。
- on_request：预订尚未确认，后续可能确认或被拒；建议使用 booking_detail 定时轮询，直到确认或拒绝。
- rejected：预订未确认或已被取消。
- failed：因第三方错误或请求技术错误导致预订失败。
- inprocess-booking：因网络/服务器问题未正确入库。遇到此状态需直接联系 Qtech 客服确认处理。

---

## 6. 静态数据与 FTP 访问

- FTP 访问信息（测试环境）：
    - 主机名：18.170.183.159
    - 用户名：colosseum_live_static_data
    - 密码：v7QAMfegDWcDBbqx

- 可下载文件：
    - static_data_cities.csv（城市）
    - static_data_countries.csv（国家）
    - static_data_hotels.csv（酒店）
    - static_data_nationality.csv（国籍）

- 规则与说明：
    - API 白名单 IP 无限制；FTP 白名单仅限 2 个 IP。
    - 测试与生产静态数据文件不同，按日期生成于对应文件夹。
    - FTP 访问信息将随测试访问邮件共享；访问前请将出网 IP 提供给对方加入白名单。
    - 静态数据建议每 15 天或每月更新一次，避免目的地数据偏差。

---

## 7. 对接过程与注意事项

- 需求确认
    - 明确通过 QTECH 技术通道拉取酒店资源，覆盖搜索、预订、取消全流程。

- 技术准备
    - 具备 RESTful/API 使用经验
    - 能处理 JSON/XML（以 JSON 为主）
    - 基本网络安全（HTTPS、鉴权、IP 白名单等）

- 对接步骤
    1. 获取接口文档与测试账号
    2. 配置测试环境，连通性检查（API 和 FTP）
    3. 实现核心接口：search/detail/cancellation_policy/reservation/booking/cancel
    4. 进行联调与功能测试（包含极端与异常场景）
    5. 输出测试报告，配合认证
    6. 切换生产环境并灰度观察

- 关键注意事项
    - agent_ref_no 每单唯一；用于查询与幂等兜底
    - 预订 expected_price 必须与搜索返回一致
    - 取消政策需匹配所订房间的最新版本
    - 预订超时（180s）后立即用 booking_detail 轮询；如仍无进展，联系 Qtech 客服
    - 充分处理网络错误、超时、第三方错误码，设计可重试与回滚策略
    - 定期同步静态数据并做差异校验（目的地/酒店/国籍等）

- 交付物
    - 已实现并通过测试的对接能力（搜索、预订、取消）
    - 技术文档与运维手册（含错误码处理、重试策略、状态流转图）
    - 监控与告警（超时率、失败率、价格不一致、状态不一致）

---

## 8. 术语约定与参数统一

- unique_id：一次搜索/详情会话的唯一 ID，由 QTECH 返回
- section_unique_id / roomClassId：房型（section/room class）的唯一标识
- agent_ref_no：我方生成的订单参考号，需全局唯一
- expected_price：按搜索返回的价格原样回传，用于一致性校验
- gzip 参数：yes/no（有些示例中为 no/yes，建议标准化为 yes/no）
- 儿童字段：Children；若有儿童，须在旅客对象中提供 age

---

