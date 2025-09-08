# HeyTrip 供应商数据标准 (supplier-data-standard-1.2.2-RELEASES) 使用手册

## 概述

`supplier-data-standard-1.2.2-RELEASES.jar` 是 HeyTrip 内部供应商对接的标准化数据包，提供了统一的接口定义、请求响应模型、枚举类型和工具类，用于规范化供应商集成开发。

## 版本信息

- **版本**: 1.2.2-RELEASES
- **GroupId**: com.heytrip
- **ArtifactId**: supplier-data-standard
- **发布时间**: 2025年

## Maven 依赖配置

```xml
<dependency>
    <groupId>com.heytrip</groupId>
    <artifactId>supplier-data-standard</artifactId>
    <version>1.2.2-RELEASES</version>
</dependency>
```

## 核心包结构

```
com.heytrip.common/
├── aonnotation/          # 注解定义
├── apiservice/           # 核心API服务接口
├── enums/               # 枚举类型定义
├── request/             # 请求模型
├── response/            # 响应模型
│   ├── base/           # 基础响应模型
│   └── other/          # 其他响应模型
├── result/             # 结果封装
└── utils/              # 工具类
```

## 核心接口

### ISupplierApiService

这是供应商对接的核心接口，定义了所有标准化的API方法：

```java
package com.heytrip.common.apiservice;

public interface ISupplierApiService {
    // 静态数据相关
    Result<List<XCountryResponse>> getCountries(String supplierType, String language);
    Result<List<XCityResponse>> getCities(String supplierType, String countryId, String language);
    Result<List<String>> getBookableHotelIds(String supplierType, int pageIndex, int pageSize, String ext);
    Result<XHotel> getHotel(String supplierType, String hotelId, String language, String ext);
    Result<List<XRoom>> getRooms(String supplierType, String hotelId, String language, String ext);
    
    // 报价相关
    Result<List<XRoom>> getPrice(XSupplierPriceRequest input);
    Result<Map<String, List<XRoom>>> getPrices(XSupplierPriceRequest input);
    
    // 订单相关
    Result<XOrderCheckResponse> orderCheck(XSupplierCheckRequest input);
    Result<XCreateOrderResponse> createOrder(XCreateOrderRequest input);
    Result<XCancelOrderResponse> cancelOrder(XCancelOrderRequest input);
    Result<XQueryOrderResponse> queryOrder(String supplierType, String distributorOrderId, String supplierOrderId, String ext);
    
    // 增量数据相关
    Result<XHotelIncrement> getHotelIncrement(String supplierType, long maxId, String query);
    Result<XRoomIncrement> getRoomIncrement(String supplierType, long maxId, String query);
    Result<XPriceCacheIncrementResponse> GetPriceCacheIncrement(String supplierType, long maxId, Long minTime, Boolean includeChangeDate, String query);
}
```

## 请求模型 (Request)

### 1. XSupplierPriceRequest - 报价请求

```java
public class XSupplierPriceRequest {
    private String supplierType;        // 供应商类型
    private String hotelId;            // 单酒店ID
    private String hotelIds;           // 多酒店ID，逗号分隔
    private String checkInDate;        // 入住日期 (yyyy-MM-dd)
    private String checkOutDate;       // 离店日期 (yyyy-MM-dd)
    private Integer roomCount;         // 房间数量
    private Integer adultCount;        // 成人数量
    private Integer childCount;        // 儿童数量
    private String childAges;          // 儿童年龄，逗号分隔
    private String currency;           // 货币代码
    private String nationality;        // 国籍
    private String language;           // 语言
    private String ext;               // 扩展参数
}
```

### 2. XSupplierCheckRequest - 验单请求

```java
public class XSupplierCheckRequest {
    private String supplierType;       // 供应商类型
    private String hotelId;           // 酒店ID
    private String roomId;            // 房型ID
    private String ratePlanId;        // 价格计划ID
    private String checkInDate;       // 入住日期
    private String checkOutDate;      // 离店日期
    private Integer roomCount;        // 房间数量
    private Integer adultCount;       // 成人数量
    private Integer childCount;       // 儿童数量
    private String childAges;         // 儿童年龄
    private String ext;              // 扩展参数
}
```

### 3. XCreateOrderRequest - 创建订单请求

```java
public class XCreateOrderRequest {
    private String supplierType;           // 供应商类型
    private String distributorOrderId;     // 分销商订单号
    private String hotelId;               // 酒店ID
    private String roomId;                // 房型ID
    private String ratePlanId;            // 价格计划ID
    private String checkInDate;           // 入住日期
    private String checkOutDate;          // 离店日期
    private Integer roomCount;            // 房间数量
    private Integer adultCount;           // 成人数量
    private Integer childCount;           // 儿童数量
    private String childAges;             // 儿童年龄
    private BigDecimal totalPrice;        // 总价格
    private String currency;              // 货币
    private String customerType;          // 客户类型
    private String payType;               // 支付类型
    private List<CreateOrderCustomer> customers; // 客户信息列表
    private String specialRequests;       // 特殊要求
    private String ext;                  // 扩展参数
    
    // 内部类：客户信息
    public static class CreateOrderCustomer {
        private String firstName;         // 名
        private String lastName;          // 姓
        private String title;            // 称谓
        private String customerType;     // 客户类型
        private Integer age;             // 年龄
    }
}
```

### 4. XCancelOrderRequest - 取消订单请求

```java
public class XCancelOrderRequest {
    private String supplierType;         // 供应商类型
    private String distributorOrderId;   // 分销商订单号
    private String supplierOrderId;      // 供应商订单号
    private String cancelReason;         // 取消原因
    private String ext;                 // 扩展参数
}
```

## 响应模型 (Response)

### 基础响应模型 (base)

#### 1. XHotel - 酒店信息

```java
public class XHotel {
    private String hotelId;              // 酒店ID
    private String hotelName;            // 酒店名称
    private String hotelNameEn;          // 酒店英文名称
    private String address;              // 地址
    private String cityId;               // 城市ID
    private String cityName;             // 城市名称
    private String countryId;            // 国家ID
    private String countryName;          // 国家名称
    private BigDecimal latitude;         // 纬度
    private BigDecimal longitude;        // 经度
    private Integer starRating;          // 星级
    private String phone;                // 电话
    private String email;                // 邮箱
    private String description;          // 描述
    private String checkInTime;          // 入住时间
    private String checkOutTime;         // 离店时间
    private List<XHotelImage> images;    // 酒店图片
    private List<XHotelFacility> facilities; // 酒店设施
    // ... 更多属性
}
```

#### 2. XRoom - 房型信息

```java
public class XRoom {
    private String roomId;               // 房型ID
    private String roomName;             // 房型名称
    private String roomNameEn;           // 房型英文名称
    private String description;          // 描述
    private Integer roomSize;            // 房间面积
    private XEnumWindow window;          // 窗户类型
    private XEnumNoSmoking smoking;      // 是否禁烟
    private XEnumBathRoomType bathRoomType; // 浴室类型
    private List<XRoomBedRoom> bedRooms; // 卧室信息
    private List<XRoomFacility> facilities; // 房间设施
    private List<XRoomImage> images;     // 房间图片
    private List<XRatePlan> ratePlans;   // 价格计划
    // ... 更多属性
}
```

#### 3. XRatePlan - 价格计划

```java
public class XRatePlan {
    private String ratePlanId;           // 价格计划ID
    private String ratePlanName;         // 价格计划名称
    private BigDecimal basePrice;        // 基础价格
    private BigDecimal totalPrice;       // 总价格
    private String currency;             // 货币
    private XMealType mealType;          // 餐食类型
    private Boolean cancelable;          // 是否可取消
    private Boolean payAtHotel;          // 是否酒店付款
    private Boolean instantConfirmation; // 是否即时确认
    private List<XCancelRule> cancelRules; // 取消规则
    private List<XBookingRule> bookingRules; // 预订规则
    private List<XRatePlanDaily> dailyRates; // 每日价格
    // ... 更多属性
}
```

### 其他响应模型 (other)

#### 1. XCreateOrderResponse - 创建订单响应

```java
public class XCreateOrderResponse {
    private String distributorOrderId;   // 分销商订单号
    private String supplierOrderId;      // 供应商订单号
    private SupplierOrderStatusEnum status; // 订单状态
    private String statusDescription;    // 状态描述
    private BigDecimal totalAmount;      // 总金额
    private String currency;             // 货币
    private String confirmationNumber;   // 确认号
    private String message;              // 消息
    private Boolean success;             // 是否成功
}
```

#### 2. XOrderCheckResponse - 验单响应

```java
public class XOrderCheckResponse {
    private Boolean available;           // 是否可预订
    private BigDecimal totalPrice;       // 总价格
    private String currency;             // 货币
    private String message;              // 消息
    private List<XCancelRule> cancelRules; // 取消规则
    private String ratePlanId;           // 价格计划ID
    private String checkToken;           // 验单令牌
}
```

#### 3. XQueryOrderResponse - 查询订单响应

```java
public class XQueryOrderResponse {
    private String distributorOrderId;   // 分销商订单号
    private String supplierOrderId;      // 供应商订单号
    private SupplierOrderStatusEnum status; // 订单状态
    private String statusDescription;    // 状态描述
    private String hotelName;            // 酒店名称
    private String roomName;             // 房型名称
    private String checkInDate;          // 入住日期
    private String checkOutDate;         // 离店日期
    private BigDecimal totalAmount;      // 总金额
    private String currency;             // 货币
    private String confirmationNumber;   // 确认号
    private List<QueryOrderDaily> dailyDetails; // 每日详情
}
```

## 枚举类型 (Enums)

### 1. 订单状态枚举

```java
public enum SupplierOrderStatusEnum {
    PENDING_CONFIRMATION(1, "待确认"),
    CONFIRMED(2, "已确认"),
    IN_PROGRESS(3, "进行中"),
    COMPLETED(4, "已完成"),
    CHECKED_IN(5, "已入住"),
    CHECKED_OUT(6, "已退房"),
    REJECTED(7, "已拒绝"),
    CANCELLATION_REQUESTED(8, "取消申请中"),
    CANCELLED(9, "已取消"),
    REFUNDED(10, "已退款");
}
```

### 2. 货币枚举

```java
public enum XEnumCurrency {
    CNY("CNY", "人民币"),
    USD("USD", "美元"),
    EUR("EUR", "欧元"),
    GBP("GBP", "英镑"),
    JPY("JPY", "日元"),
    MYR("MYR", "马来西亚林吉特"),
    SGD("SGD", "新加坡元"),
    THB("THB", "泰铢");
}
```

### 3. 餐食类型枚举

```java
public enum XMealType {
    ROOM_ONLY(0, "仅住宿"),
    BREAKFAST(1, "含早餐"),
    HALF_BOARD(2, "含早晚餐"),
    FULL_BOARD(3, "含三餐"),
    ALL_INCLUSIVE(4, "全包");
}
```

### 4. 床型枚举

```java
public enum XEnumBedType {
    SINGLE_BED(1, "单人床"),
    DOUBLE_BED(2, "双人床"),
    TWIN_BEDS(3, "双床"),
    QUEEN_BED(4, "大床"),
    KING_BED(5, "特大床"),
    SOFA_BED(6, "沙发床");
}
```

## 结果封装

### Result<T> - 统一响应结果

```java
public class Result<T> {
    private Integer code;        // 结果代码
    private String message;      // 消息
    private T data;             // 数据
    private Boolean success;     // 是否成功
    private Long timestamp;      // 时间戳
    
    // 静态方法
    public static <T> Result<T> success(T data);
    public static <T> Result<T> error(String message);
    public static <T> Result<T> error(Integer code, String message);
}
```

### PageDto<T> - 分页结果

```java
public class PageDto<T> {
    private List<T> records;     // 记录列表
    private Long total;          // 总记录数
    private Long size;           // 每页大小
    private Long current;        // 当前页
    private Long pages;          // 总页数
}
```

## 工具类 (Utils)

### 1. MD5Utils - MD5加密工具

```java
public class MD5Utils {
    public static String encrypt(String data);
    public static String encrypt(String data, String charset);
    public static boolean verify(String data, String md5);
}
```

### 2. SHA256Util - SHA256加密工具

```java
public class SHA256Util {
    public static String encrypt(String data);
    public static String encrypt(String data, String charset);
    public static boolean verify(String data, String sha256);
}
```

## 使用示例

### 1. 实现供应商API服务

```java
@Service
public class SupplierApiServiceImpl implements ISupplierApiService {
    
    @Override
    public Result<List<XRoom>> getPrice(XSupplierPriceRequest request) {
        try {
            // 调用供应商API获取价格
            List<XRoom> rooms = callSupplierPriceApi(request);
            return Result.success(rooms);
        } catch (Exception e) {
            return Result.error("获取价格失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<XCreateOrderResponse> createOrder(XCreateOrderRequest request) {
        try {
            // 调用供应商API创建订单
            XCreateOrderResponse response = callSupplierCreateOrderApi(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("创建订单失败: " + e.getMessage());
        }
    }
    
    // 实现其他方法...
}
```

### 2. 控制器使用示例

```java
@RestController
@RequestMapping("/api/supplier")
public class SupplierController {
    
    @Resource
    private ISupplierApiService supplierApiService;
    
    @GetMapping("/getPrice")
    public Result<List<XRoom>> getPrice(@ModelAttribute XSupplierPriceRequest request) {
        return supplierApiService.getPrice(request);
    }
    
    @PostMapping("/createOrder")
    public Result<XCreateOrderResponse> createOrder(@RequestBody XCreateOrderRequest request) {
        return supplierApiService.createOrder(request);
    }
}
```

### 3. 数据转换示例

```java
// 将内部酒店数据转换为标准格式
public XHotel convertToXHotel(InternalHotel internalHotel) {
    XHotel xHotel = new XHotel();
    xHotel.setHotelId(internalHotel.getId());
    xHotel.setHotelName(internalHotel.getName());
    xHotel.setAddress(internalHotel.getAddress());
    xHotel.setLatitude(internalHotel.getLatitude());
    xHotel.setLongitude(internalHotel.getLongitude());
    xHotel.setStarRating(internalHotel.getStarRating());
    
    // 转换设施信息
    List<XHotel.XHotelFacility> facilities = internalHotel.getFacilities()
        .stream()
        .map(this::convertToXHotelFacility)
        .collect(Collectors.toList());
    xHotel.setFacilities(facilities);
    
    return xHotel;
}
```

## 最佳实践

### 1. 错误处理

```java
@Override
public Result<XOrderCheckResponse> orderCheck(XSupplierCheckRequest request) {
    try {
        // 参数验证
        if (StringUtils.isBlank(request.getHotelId())) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "酒店ID不能为空");
        }
        
        // 业务逻辑处理
        XOrderCheckResponse response = processOrderCheck(request);
        
        return Result.success(response);
        
    } catch (BusinessException e) {
        log.error("验单业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    } catch (Exception e) {
        log.error("验单系统异常: {}", e.getMessage(), e);
        return Result.error(ResultCode.SYSTEM_ERROR.getCode(), "系统异常");
    }
}
```

### 2. 日志记录

```java
@Override
public Result<XCreateOrderResponse> createOrder(XCreateOrderRequest request) {
    String distributorOrderId = request.getDistributorOrderId();
    log.info("开始创建订单, distributorOrderId: {}", distributorOrderId);
    
    try {
        // 记录请求参数
        log.debug("创建订单请求参数: {}", JSON.toJSONString(request));
        
        XCreateOrderResponse response = processCreateOrder(request);
        
        // 记录响应结果
        log.info("订单创建成功, distributorOrderId: {}, supplierOrderId: {}", 
                distributorOrderId, response.getSupplierOrderId());
        
        return Result.success(response);
        
    } catch (Exception e) {
        log.error("订单创建失败, distributorOrderId: {}, error: {}", 
                distributorOrderId, e.getMessage(), e);
        return Result.error("订单创建失败: " + e.getMessage());
    }
}
```

### 3. 缓存使用

```java
@Service
public class HotelDataService {
    
    @Cacheable(value = "hotel_cache", key = "#hotelId")
    public Result<XHotel> getHotel(String supplierType, String hotelId, String language, String ext) {
        log.info("从供应商获取酒店信息: {}", hotelId);
        
        // 调用供应商API
        XHotel hotel = callSupplierHotelApi(supplierType, hotelId, language, ext);
        
        return Result.success(hotel);
    }
    
    @CacheEvict(value = "hotel_cache", key = "#hotelId")
    public void evictHotelCache(String hotelId) {
        log.info("清除酒店缓存: {}", hotelId);
    }
}
```

## 注意事项

1. **版本兼容性**: 确保使用的版本与项目中其他依赖兼容
2. **数据验证**: 在处理请求数据时，务必进行参数验证
3. **异常处理**: 统一使用 Result 封装返回结果，避免抛出未捕获异常
4. **日志记录**: 关键操作要记录详细日志，便于问题排查
5. **性能优化**: 合理使用缓存，避免重复调用供应商API
6. **安全考虑**: 敏感信息（如密钥）不要记录在日志中


---

*本文档基于 supplier-data-standard-1.2.2-RELEASES 版本编写，最后更新时间：2025-09-08*
