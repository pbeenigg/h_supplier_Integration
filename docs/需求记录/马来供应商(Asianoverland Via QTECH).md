# QTECH API 供应商对接文档（Asianoverland）
日期：2025-09-04

本文档用于指导对接 QTECH 技术通道（供应商标识：Asianoverland）的酒店业务流程与接口规范，包含环境信息、调用顺序、接口参数、示例、静态数据、状态说明及对接注意事项。

- 供应商技术通道官网：https://www.qtechsoftware.com/
- 技术服务后台（Web Service - Support）： https://colosseum.otrams.com/ws/index.php    （Heytrip_Test / Welcome@@123）
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
    - 4.3 取消规则 hotel_cancellation_policy
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
2) hotel_detail：拉取指定酒店详细信息（可选，当搜索酒店列表信息不全用）
3) hotel_cancellation_policy：获取所选房型的取消条款（强制必选， 请求预定时必须调用一次获取预定价）
4) hotel_reservation：提交预订（必选）
5) booking_detail：查询预订详情（可选，用于状态轮询与兜底）
6) get_cancellation_charges：获取当前的取消费用（必选）
7) cancel_the_booking：取消预定（必选）

注意：
- hotel_detail 与 hotel_cancellation_policy 的 unique_id/section_unique_id 来自 search/detail 响应。
- 预订时 expected_price 必须与搜索结果一致，否则会报错。
- agent_ref_no 必须对每单唯一，用于后续查询与幂等兜底。


- 通用响应定义：
```json
{
    "TotalCount": 0,
    "WebServiceVersion": "2.0",
    "Message": "fail",
    "StartTime": "2025-09-11T09:11:33.005296337Z",
    "EndTime": "2025-09-11T09:11:33.019113795Z",
    "SearchUniqueId": "824-010-20250911091133-010-983001-010-1757581893017222215-010-",
    "MessageInfo": "Nationality is mandatory."
}
```
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
    - hotel_ids：酒店 ID 列表，逗号分隔（可选） 最多支持 100个酒店 ID 查询
    - number_of_rooms：房间数（例：1）
    - roomDetails：JSON 数组，长度与房间数一致
        - numberOfAdults：成人数
        - numberOfChild：儿童数（可选）
        - ChildAge：儿童年龄，逗号分隔（可选）

- roomDetails 示例（1 房 2 成人 1 儿童，儿童 5 岁）：
```json
[
  {
    "numberOfAdults": 2,
    "numberOfChild": 1,
    "ChildAge": "5"
  }
]
```
- roomDetails 示例（1 房 2 成人，无儿童）：
```json
[
  {
    "numberOfAdults": 2
  }
]
```
- roomDetails 示例（总共：2 房，1 房 1 成人，1 房 2 成人 1 儿童，儿童 2 岁）：
```json
[
  {
    "numberOfAdults": 1
  },
  {
    "numberOfAdults": 2,
    "numberOfChild": 1,
    "ChildAge": "2"
  }
]
```


- 响应示例（精简）：
```json
{
  "TotalCount": 1,
  "WebServiceVersion": "2.0",
  "Message": "success",
  "HotelList": [
    {
      "HotelId": "OT000016097",
      "HotelName": "LE MÉRIDIEN DUBAI HOTEL & CONFERENCE CENTRE",
      "LocalHotelId": "OT000016097",
      "PropertyRating": "5.0",
      "Available": "1",
      "Latitude": "25.249048",
      "Longitude": "55.34764",
      "Address": "Airport Road PO BOX 10001",
      "ThumbNailUrl": "http://colosseum.otrams.com/cpfv3/images/?image=aHR0cHM6Ly9pLnRyYXZlbGFwaS5jb20vaG90ZWxzLzEwMDAwMDAvMTgwMDAwLzE3ODAwMC8xNzc5MzIvMDRhZjUwZmZfYi5qcGc=",
      "RateCurrencyCode": "USD",
      "TotalCharges": 115.6,
      "HotelProperty": [
        {
          "DisplayRoomRate": 115.6,
          "SectionUniqueId": "ZG90dzoxXzMwOTU0X3NpbmdsZV8xXzFfMF9kZWx1eGVyb29tX2JyZWFrZmFzdF90cnVlXzE4RF8xMDAuMDAlQ1A6cXFxOmdv",
          "Type": "Selection",
          "RoomRates": [
            {
              "Available": 1,
              "NumberOfRooms": 1,
              "NumberOfAdults": 1,
              "NumberOfChild": "0",
              "ChildAges": null,
              "RoomRate": 115.6,
              "RoomType": "Deluxe Room Breakfast",
              "RoomCategory": "Deluxe Room",
              "MealBasis": "Breakfast",
              "MealCode": "BB",
              "Note": "",
              "ClassUniqueId": "MzA5NTRfMF9zaW5nbGVfMV8xXzBfZGVsdXhlIHJvb21fYnJlYWtmYXN0X3RydWU=",
              "RateBreakup": [
                {
                  "Date": "29-09-2025",
                  "Day": "Monday",
                  "DisplayNightlyRate": 115.6
                }
              ]
            }
          ],
          "RoomDetails": [],
          "Refundable": true,
          "Policies": {
            "CancellationPolicy": [
              {
                "Start": "2025-09-28 13:00:00 +0530",
                "End": "2025-09-29 05:30:00 +0530",
                "Charges": 115.6
              }
            ]
          }
        }
      ]
    }
  ],
  "StartTime": "2025-09-11T08:52:48.58352284Z",
  "EndTime": "2025-09-11T08:52:56.406280267Z",
  "SearchUniqueId": "824-010-20250911085248-010-982996-010-1757580768597119691-010-"
}
```

- 字段说明要点：
    - SearchUniqueId：本次搜索会话唯一标识，后续 detail/cancellation/reservation 需传
    - SectionUniqueId：房型组合唯一标识（后续取消规则与预订使用）
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
      | SectionUniqueId | string | "25_181195-2_1_93642" | 房型组合唯一ID（后续取消规则/预订必传） |

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
  "HotelId": "OT000016097",
  "HotelName": "LE MÉRIDIEN DUBAI HOTEL & CONFERENCE CENTRE",
  "Description": "",
  "Amenities": {
    "HotelAmenities": [
      {
        "AmenityName": ""
      }
    ],
    "RoomAmenities": [
      {
        "RoomAmenityName": ""
      }
    ]
  },
  "email": " ",
  "website": " ",
  "Phone": "",
  "longitude": "55.34764",
  "latitude": "25.249048",
  "HotelRating": "5.0",
  "HotelAddress": "Airport Road PO BOX 10001",
  "HotelImages": [
    {
      "ThumbnailUrl": "",
      "BigUrl": ""
    }
  ],
  "SectionSelection": [
    {
      "DisplayRoomRate": 778.37,
      "Type": "Selection",
      "SectionUniqueId": "ZG90dzoxXzMwOTU0X2RvdWJsZXBsdXNjaGlsZF8xXQOnFxcTpnbw==",
      "RoomDetails": [

      ],
      "RoomRates": [
        {
          "Available": "1",
          "NumberOfRooms": 1,
          "NumberOfAdults": 2,
          "NumberOfChild": 1,
          "RoomRate": 778.37,
          "RoomType": "Junior Suite, Garden View Full Board  - Dynamic Availability ",
          "RoomCategory": "JUNIOR SUITE, GARDEN VIEW",
          "MealBasis": "Full Board",
          "ClassUniqueId": "MzA5NTRfMF9kb3VibGVwbHVzY2hpbGRfMV8yXzFfNV9qdW5pb3IfdHJ1ZQ==",
          "RefundPolicyText": "Refundable",
          "RateBreakup": [
            {
              "Date": "30-10-2025",
              "Day": "Thursday",
              "DisplayNightlyRate": 778.37
            }
          ]
        }
      ]
    }
  ],
  "StartTime": "2025-09-11 07:07:37",
  "EndTime": "2025-09-11 07:07:43"
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
    | SectionUniqueId | string | "25_..." | 组合ID（后续取消规则/预订用） |
    | RoomRates[].ClassUniqueId | string | "181195_..." | 房类ID（预订用） |
    | RoomRates[].Available | string/int | "1" | 是否可订 |
    | RoomRates[].RoomType | string | "Single-..." | 房型描述 |
    | RoomRates[].MealBasis | string | "Room Only" | 餐型 |
---

### 4.3 取消规则 hotel_cancellation_policy
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
    "CancellationCurrency": "USD",
    "TotalBookingAmount": 115.59,
    "ContractComment": " No shows policy will be applicable for 100 % Room ....",
    "RefundPolicyText": "Refundable",
    "Policies": {
        "CancellationPolicy": [
            {
                "Start": "2025-09-21 21:00:00 +0530",
                "End": "2025-09-29 06:59:59 +0530",
                "Charges": 115.59,
                "Remark": ""
            },
            {
                "Start": "2025-09-29 07:00:00 +0530",
                "End": "2025-09-29 05:30:00 +0530",
                "Charges": 115.59,
                "Remark": ""
            }
        ],
        "AmmendmentPolicy": "",
        "NoShowPolicy": ""
    },
    "BookingAllowedInfo": {
        "PriceChange": "no",
        "PriceDiff": 0,
        "RateChanges": [],
        "PayNow": "no",
        "BookingAllowed": "yes",
        "MessageInfo": "You can proceed with the booking.",
        "Message": "success",
        "SoldOut": "No"
    },
    "StartTime": "2025-09-11 03:47:31",
    "EndTime": "2025-09-11 03:47:41"
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
    - 使用最近的取消规则且与所订房间一致，否则会报错。
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
    - expected_price：需与 TotalBookingAmount 一致（来自取消规则响应）
    - roomDetails：JSON 数组，长度与预订房间数一致
        - numberOfAdults：成人数
        - numberOfChilds：儿童数
        - roomClassId：具体房类 ID（来自搜索/详情响应）
        - passangers：乘客列表，长度与成人+儿童数一致
            - salutation：称谓（Mr/Ms/Mrs/Children）
            - first_name 名
            - last_name： 姓
            - age：年龄（儿童必填）

```
- 参数示例（roomDetails） （公共：1 房，2 成人 1 儿童，儿童 5 岁）
```json
[
  {
    "numberOfChilds": "1",
    "roomClassId": "0_0_73179_9840781",
    "passangers": [
      {
        "salutation": "MR",
        "first_name": "Himanshu",
        "last_name": "Test"
      },
      {
        "salutation": "MR",
        "first_name": "Prachi",
        "last_name": "shenoy"
      },
      {
        "salutation": "Child",
        "first_name": "Tanu",
        "last_name": "Prabhu",
        "age": "5"
      }
    ]
  }
]
```

- roomDetails 示例 （总共：2 房，1 房 1 成人，1 房 2 成人 1 儿童，儿童 2 岁）
```json
[
  {
    "numberOfChilds": "1",
    "roomClassId": "0_1_73100_5630075",
    "passangers": [
      {
        "salutation": "MR",
        "first_name": "sachin",
        "last_name": "Test"
      },
      {
        "salutation": "MR",
        "first_name": "virat",
        "last_name": "Test"
      },
      {
        "salutation": "Child",
        "first_name": "tanu",
        "last_name": "prabhu",
        "age": "2"
      }
    ]
  },
  {
    "numberOfChilds": "0",
    "roomClassId": "0_0_73100_358796",
    "passangers": [
      {
        "salutation": "MR",
        "first_name": "Himanshu",
        "last_name": "Test"
      }
    ]
  }
]
```


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

- 通过FTP可下载文件：
    - static_data_cities.csv（城市）
    - static_data_countries.csv（国家）
    - static_data_hotels.csv（酒店）
    - static_data_nationality.csv（国籍）
  
- 通过文件导入方式   
- static_data_hotels_giata.csv（GIATA 酒店映射关系）


供应商提供的静态数据文件均为 CSV 格式，UTF-8 编码
- 国籍字段说明： code,nationality,iso_code
- 城市字段说明： name,city_code,country_code,country_name
- 国家字段说明： country_code、country_name
- 酒店字段说明： id,name,city_code,city_name,country_code,main_image,short_desc,latitude,longitude,rating,address,phone,website,long_desc
- GIATA酒店映射：Id,giata_id,name,city_code,city_name,country_code,long_desc,latitude,longitude,rating,address,main_image


- 流程设计： 
  - 每个供应商适配器都需要实现一种通过 FTP的方式同步静态数据到数据库对应的表。
  - 服务启动时，根据不同供应商适配器读取不同供应商配置表信息(ftpConfig字段)，获取FTP服务器连接方式，下载最新的静态数据文件并解析入库。
  - 解析时，若遇到重复的主键（如酒店ID），则覆盖更新，否则插入新数据。
  - 若FTP连接失败或文件下载失败，则记录错误日志并告警，保持上次成功的数据。
  - 解析完成后，记录同步时间和数据量，便于后续监控和排查。
  - 建议静态数据同步定时任务，还需每15天执行一次，保持数据新鲜度。
  - 实现完成同步数据逻辑后，还需在对应的供应商适配器中实现静态数据查询接口，供上层服务调用（目前只需要实现 AsianOverlandAdapter 适配器 ）。
  - 静态数据查询接口包括：根据酒店ID查询酒店信息、根据城市代码查询城市信息、根据国家代码查询国家信息、根据国籍代码查询国籍信息等。（ PS:都需要支持分页查询和条件查询）
  - 静态数据查询接口还需要增加Caffeine本地缓存策略，避免频繁查询数据库，服务启动需要预热缓存，定时任务同步数据后也需要更新缓存。

- 思考讨论：
   - 再项目中不同供应商适配器的静态数据文件格式可能不同，而且不同适配器获取的数据都要严格区分开，避免数据混淆。
   - 是否需设计统一的解析接口和适配器模式，便于后续扩展和维护。支持不同供应商的静态数据文件格式。
   - 是否考虑根据不同供应商的静态数据文件格式，设计不同的解析策略和实现类。
   - 是否要考虑根据不同供应商的数据，分表存储，避免数据量过大影响查询性能。
   - 静态数据文件的下载和解析过程可能耗时较长，需考虑异步处理和超时重试机制。
   - 静态数据文件的下载和解析过程可能失败，需考虑错误处理和日志记录，便于排查问题。
   - 静态数据文件可能较大，需考虑内存溢出和性能问题。




供应商对接标准实体定义： supplier-data-standard-1.2.2-RELEASES:com.heytrip.common
- XCityResponse  城市
- XCountryResponse  国家
- XHotel   酒店
- XRoom 房型
- XRatePlan  价格计划
- XRatePlanDaily 价格计划日历








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
    - 取消规则需匹配所订房间的最新版本
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

