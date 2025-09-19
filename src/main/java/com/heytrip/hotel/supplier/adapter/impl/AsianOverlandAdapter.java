package com.heytrip.hotel.supplier.adapter.impl;

import cn.hutool.core.util.StrUtil;
import com.heytrip.common.enums.XEnumCurrency;
import com.heytrip.common.enums.XEnumNoSmoking;
import com.heytrip.common.request.XSupplierCheckRequest;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRatePlanDaily;
import com.heytrip.common.response.other.*;
import com.heytrip.common.result.Result;
import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import com.heytrip.hotel.supplier.adapter.builder.QTechQueryBuilder;
import com.heytrip.hotel.supplier.adapter.capability.StaticBridge;
import com.heytrip.hotel.supplier.adapter.service.StaticDataQueryService;
import com.heytrip.hotel.supplier.client.HttpClientService;
import com.heytrip.hotel.supplier.dto.qtech.req.*;
import com.heytrip.hotel.supplier.dto.qtech.resp.*;
import com.heytrip.hotel.supplier.dto.supplier.SupplierAuth;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.request.XCreateOrderRequest;
import com.heytrip.common.request.XCancelOrderRequest;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import com.heytrip.hotel.supplier.adapter.capability.PricingBridge;
import com.heytrip.hotel.supplier.adapter.capability.OrderBridge;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


/**
 * Asianoverland Via QTECH 供应商适配器实现
 * <p>
 * 实现QTECH API的酒店搜索、预订、取消等核心功能
 * API文档参考：docs/需求记录/马来供应商(Asianoverland Via QTECH).md
 *
 * @author Pax
 */
@Component
public class AsianOverlandAdapter extends AbstractSupplierAdapter implements PricingBridge, OrderBridge, StaticBridge {


    @Resource
    private HttpClientService httpClientService;

    @Resource
    private StaticDataQueryService staticDataQueryService;

    // 默认供应商信息（当数据库配置不可用时使用）
    private static final Long DEFAULT_SUPPLIER_ID = 1L;
    private static final String DEFAULT_SUPPLIER_NAME = "AsianOverland";
    private static final String DEFAULT_SUPPLIER_CODE = "AsianOverland";

    // QTECH API 地址
    private static final String SEARCH_BASE_URL = "http://colosseum.otrams.com:8087";
    private static final String API_BASE_URL = "https://colosseum.otrams.com";


    // 支持的城市列表（可扩展）
    private static final List<String> SUPPORTED_CITIES = Arrays.asList(
            "Kuala Lumpur", "Penang", "Johor Bahru", "Malacca", "Ipoh", "Kota Kinabalu", "Kuching",
            "Dubai", "Singapore", "Bangkok", "Manila", "Jakarta"
    );


    /**
     * 初始化适配器
     */
    @PostConstruct
    public void init() {
        initialize();
    }


    /**
     * 获取供应商标识符
     * 用于从数据库加载供应商配置，避免循环依赖
     */
    @Override
    public String getSupplierCode() {
        return DEFAULT_SUPPLIER_CODE; // 供应商代码，用于数据库查询
    }


    /**
     * 获取当前供应商配置
     */
    public SupplierConfig getSupplierConfig() {
        return supplierConfig;
    }


    /**
     * 兼容测试：获取支持的城市列表（来自供应商配置）
     */
    public String getSupportedCities() {
        return supplierConfig != null ? supplierConfig.getSupportedCities() : null;
    }

    /**
     * 兼容测试：获取支持的国家列表（来自供应商配置）
     */
    public String getSupportedCountries() {
        return supplierConfig != null ? supplierConfig.getSupportedCountries() : null;
    }


    /**
     * 安全获取供应商ID（优先使用数据库配置，否则使用默认值）
     */
    private Long getSafeSupplierId() {
        Long supplierId = getSupplierId();
        return supplierId != null ? supplierId : DEFAULT_SUPPLIER_ID;
    }

    /**
     * 安全获取供应商名称（优先使用数据库配置，否则使用默认值）
     */
    private String getSafeSupplierName() {
        String supplierName = getSupplierName();
        return StrUtil.isNotBlank(supplierName) ? supplierName : DEFAULT_SUPPLIER_NAME;
    }


    /**
     * 检查是否支持指定城市
     */
    @Override
    public boolean supportsCity(String city) {
        return SUPPORTED_CITIES.stream()
                .anyMatch(supportedCity -> supportedCity.equalsIgnoreCase(city));
    }


    /**
     * 检查是否支持指定国家
     */
    @Override
    public boolean supportsCountry(String country) {
        // 目前仅支持马来西亚、新加坡、阿联酋、泰国
        List<String> supportedCountries = Arrays.asList("Malaysia", "Singapore", "UAE", "Thailand");
        return supportedCountries.stream()
                .anyMatch(supportedCountry -> supportedCountry.equalsIgnoreCase(country));
    }


    /**
     * 优先级设置
     * 数值越小优先级越高
     */
    @Override
    public int getPriority() {
        return 10; // 高优先级
    }


    /**
     * 统一的HTTP GET请求方法，自动应用认证头部
     *
     * @param baseUrl      基础URL
     * @param endpoint     端点路径
     * @param responseType 响应类型
     * @return 响应结果
     */
    protected <T> Mono<T> executeGetRequest(String baseUrl, String endpoint, Class<T> responseType) {
        return httpClientService.get(
                baseUrl,
                endpoint,
                responseType,
                headers -> {
                    // 应用通用的认证头部
                    headers.header("Content-Type", "application/json");
                    headers.header("User-Agent", "HeyTrip-AsianOverland-Adapter-Pax/1.0");
                    headers.header("X-Supplier", getSafeSupplierName());
                },
                getSafeSupplierId()
        );
    }


    /**
     * 执行QTECH酒店搜索
     *
     * @param request 搜索请求对象
     * @return 搜索结果
     */
    public Mono<QTechSearchResponse> searchHotels(QTechSearchRequest request) {
        logger.info("开始QTECH酒店搜索，目的地: {}, 入住: {}, 离店: {}",
                request.getSelCity(), request.getCheckinDate(), request.getCheckoutDate());

        try {
            // 获取动态认证配置并设置到请求DTO中
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            request.setUsername(authConfig.getUsername());
            request.setPassword(authConfig.getPassword());

            // 校验数据一致性
            if (!request.isRoomDetailsValid()) {
                String msg = request.getRoomDetailsValidationError();
                logger.error("房间校验失败: {}", msg);
                throw new IllegalArgumentException("数据校验失败: " + msg);
            }

            logger.debug("调用QTECH搜索API，目的地: {}, 入住: {}, 离店: {}",
                    request.getSelCity(), request.getCheckinDate(), request.getCheckoutDate());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的搜索端点: {}", endpoint);

            return executeGetRequest(SEARCH_BASE_URL, endpoint, QTechSearchResponse.class);

        } catch (Exception e) {
            logger.error("QTECH搜索请求失败", e);
            return Mono.error(new RuntimeException("搜索请求失败: " + e.getMessage()));
        }
    }

    /**
     * 执行QTECH酒店预订
     *
     * @param request 预订请求对象
     * @return 预订结果
     */
    public Mono<QTechReservationResponse> bookHotel(QTechReservationRequest request) {
        logger.info("开始QTECH酒店预订，酒店ID: {}, 房间ID: {}, 订单号: {}",
                request.getHotelId(), request.getSectionUniqueId(), request.getAgentRefNo());

        try {
            // 1. 先获取取消规则（必需步骤）
            QTechCancellationPolicyRequest policyRequest = new QTechCancellationPolicyRequest();
            policyRequest.setHotelId(request.getHotelId());
            policyRequest.setUniqueId(request.getUniqueId());
            policyRequest.setSectionUniqueId(request.getSectionUniqueId());

            return getCancellationPolicy(policyRequest)
                    .flatMap(policy -> {
                        if (policy == null || !"success".equals(policy.getMessage())) {
                            return Mono.error(new RuntimeException("获取取消规则失败,无法进行预定"));
                        }

                        if (policy == null || policy.getTotalBookingAmount() == null) {
                            return Mono.error(new RuntimeException("获取取消规则失败,无法获取预定价格"));
                        }

                        QTechCancellationPolicyResponse.BookingAllowedInfo allowedInfo = policy.getBookingAllowedInfo();
                        if (allowedInfo == null || !"yes".equalsIgnoreCase(allowedInfo.getBookingAllowed())) {
                            return Mono.error(new RuntimeException("当前房型不可预订"));
                        }

                        // 2. 执行预订
                        return executeReservation(request, policy);
                    })
                    .doOnSuccess(result -> logger.info("QTECH预订完成，状态: {}",
                            result != null ? result.getStatus() : "未知"))
                    .doOnError(error -> logger.error("QTECH预订失败", error));

        } catch (Exception e) {
            logger.error("QTECH预订请求构建失败", e);
            return Mono.error(new RuntimeException("预订请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取QTECH酒店详情
     *
     * @param request 酒店详情请求对象
     * @return 酒店详情
     */
    public Mono<QTechHotelDetailResponse> getHotelDetail(QTechHotelDetailRequest request) {
        logger.info("开始获取QTECH酒店详情，酒店ID: {}", request.getHotelId());

        try {
            // 获取动态认证配置并设置到请求DTO中
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            request.setUsername(authConfig.getUsername());
            request.setPassword(authConfig.getPassword());

            logger.debug("调用QTECH酒店详情API，酒店ID: {}", request.getHotelId());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的酒店详情端点: {}", endpoint);

            return executeGetRequest(API_BASE_URL, endpoint, QTechHotelDetailResponse.class)
                    .doOnSuccess(result -> logger.info("QTECH酒店详情获取完成，酒店ID: {}", request.getHotelId()))
                    .doOnError(error -> logger.error("QTECH酒店详情获取失败，酒店ID: {}", request.getHotelId(), error));

        } catch (Exception e) {
            logger.error("QTECH酒店详情请求构建失败", e);
            return Mono.error(new RuntimeException("酒店详情请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取QTECH预订详情
     *
     * @param request 预订详情请求对象
     * @return 预订详情
     */
    public Mono<QTechBookingDetailResponse> getBookingDetail(QTechBookingDetailRequest request) {
        logger.info("开始获取QTECH预订详情，预订ID: {}", request.getBookingId());

        try {
            // 获取动态认证配置并设置到请求DTO中
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            request.setUsername(authConfig.getUsername());
            request.setPassword(authConfig.getPassword());

            logger.debug("调用QTECH预订详情API，预订ID: {}", request.getBookingId());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的预订详情端点: {}", endpoint);

            return executeGetRequest(API_BASE_URL, endpoint, QTechBookingDetailResponse.class)
                    .doOnSuccess(result -> logger.info("QTECH预订详情获取完成，预订ID: {}", request.getBookingId()))
                    .doOnError(error -> logger.error("QTECH预订详情获取失败，预订ID: {}", request.getBookingId(), error));

        } catch (Exception e) {
            logger.error("QTECH预订详情请求构建失败", e);
            return Mono.error(new RuntimeException("预订详情请求构建失败: " + e.getMessage()));
        }
    }

    /**
     * 执行QTECH取消预订
     *
     * @param request 取消请求对象
     * @return 取消结果
     */
    public Mono<QTechCancellationResponse> cancelBooking(QTechCancellationBookingRequest request) {
        logger.info("开始QTECH取消预订，预订ID: {}", request.getBookingId());

        try {

            QTechGetCancellationChargesRequest chargesRequest = new QTechGetCancellationChargesRequest();
            chargesRequest.setBookingId(request.getBookingId());
            chargesRequest.setBookingReference(request.getBookingReference());
            // 1. 先获得取消费用
            return getCancellationCharges(chargesRequest)
                    .flatMap(chargesResult -> {
                        if (chargesResult == null || !"success".equals(chargesResult.getStatus())) {
                            return Mono.error(new RuntimeException("获取取消费用失败"));
                        }

                        // 2. 执行取消
                        return executeCancellation(request);
                    })
                    .doOnSuccess(result -> logger.info("QTECH取消完成，状态: {}",
                            result != null ? result.getStatus() : "未知"))
                    .doOnError(error -> logger.error("QTECH取消失败", error));

        } catch (Exception e) {
            logger.error("QTECH取消请求构建失败", e);
            return Mono.error(new RuntimeException("取消请求构建失败: " + e.getMessage()));
        }
    }


    /**
     * 获取取消规则 （获取的精准最新的预定价格）
     *
     * @param request 取消规则请求DTO
     * @return
     */
    private Mono<QTechCancellationPolicyResponse> getCancellationPolicy(QTechCancellationPolicyRequest request) {
        return executeCancellationPolicy(request);
    }

    /**
     * 执行取消规则查询
     */
    private Mono<QTechCancellationPolicyResponse> executeCancellationPolicy(QTechCancellationPolicyRequest request) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        logger.debug("调用QTECH取消规则API，酒店ID: {}, 房型ID: {}",
                request.getHotelId(), request.getSectionUniqueId());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的取消规则端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechCancellationPolicyResponse.class);
    }

    /**
     * 执行预订
     */
    private Mono<QTechReservationResponse> executeReservation(QTechReservationRequest request,
                                                              QTechCancellationPolicyResponse policy) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        // 设置预期价格（从取消规则响应中获取）
        if (policy != null && policy.getTotalBookingAmount() != null) {
            request.setExpectedPrice(policy.getTotalBookingAmount());
        }

        /**
         * TODO
         *  1、为预订步骤保持180秒的超时时间。
         *  2、如果在180秒内没有收到响应，请立即使用booking_detail API（在预订详情API请求中传递agent_ref_no）检查此预订的状态，
         *  3、然而如果您仍然无法收到响应或跟踪预订状态，立即检查此预订。
         */

        logger.debug("调用QTECH预订API，酒店ID: {}, 房型ID: {}, 订单号: {}",
                request.getHotelId(), request.getSectionUniqueId(), request.getAgentRefNo());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的预订端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechReservationResponse.class);
    }

    /**
     * 获取酒店预定取消费用
     *
     * @param request
     * @return
     */
    private Mono<QTechCancellationChargesResponse> getCancellationCharges(QTechGetCancellationChargesRequest request) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        logger.debug("调用QTECH取消费用API，预订ID: {}", request.getBookingId());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的取消费用端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechCancellationChargesResponse.class);
    }

    /**
     * 执行取消
     */
    private Mono<QTechCancellationResponse> executeCancellation(QTechCancellationBookingRequest request) {
        // 获取动态认证配置并设置到请求DTO中
        SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
        request.setUsername(authConfig.getUsername());
        request.setPassword(authConfig.getPassword());

        logger.debug("调用QTECH取消预订API，预订ID: {}", request.getBookingId());

        String endpoint = QTechQueryBuilder.buildEndpoint(request);
        logger.debug("构建的取消预订端点: {}", endpoint);

        return executeGetRequest(API_BASE_URL, endpoint, QTechCancellationResponse.class);
    }


    // ============================================ 报价与订单 ============================================

    /**
     * 单酒店报价桥接
     */
    public List<XRoom> getPrice(XSupplierPriceRequest input) {
        logger.info("[AsianOverlandAdapter.getPrice] 开始桥接, input={}", input);

        try {
            // 组装 QTechSearchRequest
            QTechSearchRequest req = new QTechSearchRequest();

            // 日期格式转换 yyyy-MM-dd -> dd/MM/yyyy
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.getPrice] 输入缺少酒店ID，无法报价");
                return Collections.emptyList();
            }
            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            //从当前酒店详细里获取 : 目的地国家/目的地城市/国籍/居住国
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), input.getHotelId())
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country = hotel.getCountryCode();
                            //String country = "138"; //TODO  测试
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        } else {
                            throw new IllegalArgumentException("酒店ID无效，无法获取酒店信息");
                        }
                    });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);

            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索
            QTechSearchResponse resp = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getPrice] 酒店搜索失败", e))
                    .block();

            if (resp == null) {
                logger.warn("[AsianOverlandAdapter.getPrice] QTECH无响应，返回空结果");
                return Collections.emptyList();
            }
            if (!"success".equalsIgnoreCase(resp.getMessage())) {
                logger.warn("[AsianOverlandAdapter.getPrice] QTECH返回非成功: message={}, info={}", resp.getMessage(), resp.getMessageInfo());
                return Collections.emptyList();
            }

            // 3. 将 QTechSearchResponse 转换为 List<XRoom> - 使用共用转换方法

            List<QTechSearchResponse.Hotel> hotelList = resp.getHotelList();
            if (hotelList == null || hotelList.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.getPrice] 返回成功但无酒店数据");
                return Collections.emptyList();
            }

            // 仅处理指定酒店ID的报价
            Optional<QTechSearchResponse.Hotel> targetHotelOpt = hotelList.stream()
                    .filter(h -> input.getHotelId().equals(h.getHotelId()))
                    .findFirst();
            if (targetHotelOpt.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.getPrice] 返回酒店列表中不包含请求的酒店ID: {}", input.getHotelId());
                return Collections.emptyList();
            }

            // 使用共用的转换方法
            QTechSearchResponse.Hotel targetHotel = targetHotelOpt.get();
            List<XRoom> xRooms = convertHotelToXRooms(targetHotel, input);

            if (xRooms.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.getPrice] 酒店{}转换后无有效房型数据", targetHotel.getHotelId());
            } else {
                logger.info("[AsianOverlandAdapter.getPrice] 单酒店报价完成，酒店{}返回{}个房型",
                        targetHotel.getHotelId(), xRooms.size());
            }

            return xRooms;

        } catch (Exception ex) {
            logger.error("[AsianOverlandAdapter.getPrice] 获取报价失败", ex);
            return Collections.emptyList();
        }
    }


    /**
     * 多酒店报价桥接
     *
     * @param input 报价请求参数
     * @return
     */
    public Map<String, List<XRoom>> getPrices(XSupplierPriceRequest input) {
        try {
            // 组装 QTechSearchRequest
            QTechSearchRequest req = new QTechSearchRequest();
            // 日期格式转换
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.getPriceOrig] 输入缺少酒店ID，无法报价");
                throw new IllegalArgumentException("输入缺少酒店ID，无法报价");
            }

            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            // 设置国家信息（简化处理，使用固定值）
            String country = "138"; // 测试用国家代码
            req.setSelNationality(country);
            req.setCountryOfResidence(country);


            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);

            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索
            QTechSearchResponse response = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getPrices] 酒店搜索失败", e))
                    .block();

            if (response == null) {
                logger.warn("[AsianOverlandAdapter.getPrices] QTECH无响应，返回空结果");
                return new HashMap<>();
            }
            if (!"success".equalsIgnoreCase(response.getMessage())) {
                logger.warn("[AsianOverlandAdapter.getPrices] QTECH返回非成功: message={}, info={}", response.getMessage(), response.getMessageInfo());
                return new HashMap<>();
            }

            // 3. 将 QTechSearchResponse 转换为 Map<String, List<XRoom>>  格式： 酒店ID, 房型列表
            Map<String, List<XRoom>> result = new HashMap<>();

            List<QTechSearchResponse.Hotel> hotelList = response.getHotelList();
            if (hotelList == null || hotelList.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.getPrices] 返回成功但无酒店数据");
                return result;
            }

            // 处理每个酒店的报价数据
            for (QTechSearchResponse.Hotel hotel : hotelList) {
                String hotelId = hotel.getHotelId();
                List<XRoom> xRooms = convertHotelToXRooms(hotel, input);

                if (!xRooms.isEmpty()) {
                    result.put(hotelId, xRooms);
                    logger.debug("[AsianOverlandAdapter.getPrices] 酒店{}转换完成，房型数量：{}", hotelId, xRooms.size());
                } else {
                    logger.warn("[AsianOverlandAdapter.getPrices] 酒店{}无有效房型数据", hotelId);
                }
            }

            logger.info("[AsianOverlandAdapter.getPrices] 多酒店报价完成，返回{}个酒店的报价", result.size());
            return result;

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.getPriceOrig] 获取原始报价失败", e);
            throw new RuntimeException("获取原始报价失败: " + e.getMessage());
        }
    }


    /**
     * 创建订单桥接（占位实现）
     */
    public XCreateOrderResponse createOrder(XCreateOrderRequest input) {
        logger.info("[AsianOverlandAdapter.createOrder] 占位实现, input={}", input);
        return null;
    }

    /**
     * 取消订单桥接（占位实现）
     */
    public XCancelOrderResponse cancelOrder(XCancelOrderRequest input) {
        logger.info("[AsianOverlandAdapter.cancelOrder] 占位实现, input={}", input);
        return null;
    }

    /**
     * 查询订单桥接（占位实现）
     */
    public XQueryOrderResponse queryOrder(String distributorOrderId, String supplierOrderId, String ext) {
        logger.info("[AsianOverlandAdapter.queryOrder] 占位实现, distributorOrderId={}, supplierOrderId={}, ext={}", distributorOrderId, supplierOrderId, ext);
        return null;
    }


    /**
     * 获取原始单酒店报价（供应商原始数据格式）
     * 返回 QTech 供应商的原始搜索响应数据
     */
    @Override
    public Object getPriceOrig(XSupplierPriceRequest input) {
        logger.info("[AsianOverlandAdapter.getPriceOrig] 获取原始报价数据, input={}", input);

        try {
            // 组装 QTechSearchRequest
            QTechSearchRequest req = new QTechSearchRequest();
            // 日期格式转换
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.getPriceOrig] 输入缺少酒店ID，无法报价");
                throw new IllegalArgumentException("输入缺少酒店ID，无法报价");
            }

            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            // 设置国家信息
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), req.getHotelIds())
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country = hotel.getCountryCode();
                            //String country = "138"; //TODO  测试
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        } else {
                            throw new IllegalArgumentException("酒店ID无效，无法获取酒店信息");
                        }
                    });

            // 房间明细
            req.setRoomDetails(HeyUtil.buildQTechRoomDetails(input.getOccupancy()));

            // 2. 调用 QTECH 搜索
            QTechSearchResponse response = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getPrice] 酒店搜索失败", e))
                    .block();

            if (response != null && response.getStatus() != null && response.getStatus().equals("Success")) {
                logger.info("[AsianOverlandAdapter.getPriceOrig] 成功获取原始报价数据");
                return response; // 返回原始响应对象
            } else {
                logger.warn("[AsianOverlandAdapter.getPriceOrig] 供应商返回失败状态: {}",
                        response != null ? response.getStatus() : "null");
                return response;
            }

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.getPriceOrig] 获取原始报价失败", e);
            throw new RuntimeException("获取原始报价失败: " + e.getMessage());
        }
    }

    /**
     * 获取原始多酒店报价（供应商原始数据格式）
     * 当前 QTech API 支持多酒店ID查询，直接复用单酒店逻辑
     */
    @Override
    public Object getPricesOrg(XSupplierPriceRequest input) {
        logger.info("[AsianOverlandAdapter.getPricesOrg] 获取多酒店原始报价数据, input={}", input);

        // QTech API 支持传入多个酒店ID（逗号分隔），直接复用 getPriceOrig
        return getPriceOrig(input);
    }


    /**
     * 订单前置校验（标准格式）
     * 在正式下单前校验房型可售性、价格变化等
     */
    @Override
    public XOrderCheckResponse orderCheck(XSupplierCheckRequest input) {
        logger.info("[AsianOverlandAdapter.orderCheck] 执行订单前置校验, input={}", input);

        try {
            // 1. 先获取最新报价数据
            // 组装 QTechSearchRequest
            QTechSearchRequest req = new QTechSearchRequest();

            // 日期格式转换 yyyy-MM-dd -> dd/MM/yyyy
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 输入缺少酒店ID，无法报价");
                throw new IllegalArgumentException("输入缺少酒店ID");
            }
            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            //从当前酒店详细里获取 : 目的地国家/目的地城市/国籍/居住国
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), input.getHotelId())
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country = hotel.getCountryCode();
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        } else {
                            throw new IllegalArgumentException("酒店ID无效，无法获取酒店信息");
                        }
                    });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);

            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索
            QTechSearchResponse searchResponse = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheck] 酒店搜索失败", e))
                    .block();

            if (searchResponse == null) {
                logger.warn("[AsianOverlandAdapter.orderCheck] QTECH无响应，返回空结果");
                throw new IllegalStateException("QTECH接口无响应");
            }
            if (!"success".equalsIgnoreCase(searchResponse.getMessage())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] QTECH返回非成功: message={}, info={}", searchResponse.getMessage(), searchResponse.getMessageInfo());
                throw new IllegalStateException("QTECH返回非成功: " + searchResponse.getMessage());
            }

            List<QTechSearchResponse.Hotel> hotelList = searchResponse.getHotelList();
            if (hotelList == null || hotelList.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 返回成功但无酒店数据");
                throw new IllegalStateException("返回成功但无酒店数据");
            }

            // 仅处理指定酒店ID的报价
            Optional<QTechSearchResponse.Hotel> targetHotelOpt = hotelList.stream()
                    .filter(h -> input.getHotelId().equals(h.getHotelId()))
                    .findFirst();
            if (targetHotelOpt.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 返回酒店列表中不包含请求的酒店ID: {}", input.getHotelId());
                throw new IllegalStateException("返回酒店列表中不包含请求的酒店ID: " + input.getHotelId());
            }

            // 使用共用的转换方法
            QTechSearchResponse.Hotel targetHotel = targetHotelOpt.get();
            List<XRoom> xRooms = convertHotelToXRooms(targetHotel, input);

            if (xRooms.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 酒店{}转换后无有效房型数据", targetHotel.getHotelId());
                throw new IllegalStateException("无可用房型");
            }
            logger.info("[AsianOverlandAdapter.orderCheck] 单酒店报价完成，酒店:{} 返回:{}个房型,总价:{}", targetHotel.getHotelId(), xRooms.size(), targetHotel.getTotalCharges());


            ///TODO 获取搜索唯一标识（后边调用取消规则接口需要用到） 每个搜索唯一ID只能用于一次预订，并且从搜索时间起20分钟内有效
            String searchUniqueId = searchResponse.getSearchUniqueId();

            // 3. 调用取消规则接口获取最新的预定价格和取消规则
            QTechCancellationPolicyRequest policyRequest = new QTechCancellationPolicyRequest();
            policyRequest.setHotelId(targetHotel.getHotelId());
            policyRequest.setUniqueId(searchUniqueId);
            policyRequest.setSectionUniqueId(input.getRoomId());
            
            QTechCancellationPolicyResponse policyResponse = getCancellationPolicy(policyRequest)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheck] 获取酒店取消规则失败", e))
                    .block();

            if (policyResponse == null) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 取消规则接口无响应");
                throw new IllegalStateException("取消规则接口无响应");
            }
            if (!"success".equalsIgnoreCase(policyResponse.getMessage())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 取消规则接口返回非成功: message={}, info={}", 
                           policyResponse.getMessage(), policyResponse.getMessageInfo());
                throw new IllegalStateException("取消规则接口返回非成功: " + policyResponse.getMessage());
            }

            // 4. 验证预订允许状态
            QTechCancellationPolicyResponse.BookingAllowedInfo bookingInfo = policyResponse.getBookingAllowedInfo();
            if (bookingInfo == null || !"yes".equalsIgnoreCase(bookingInfo.getBookingAllowed())) {
                String reason = bookingInfo != null ? bookingInfo.getMessage() : "未知原因";
                logger.warn("[AsianOverlandAdapter.orderCheck] 房型不允许预订: {}", reason);
                throw new IllegalStateException("房型不允许预订: " + reason);
            }

            // 5. 退订状态
            if (!"Refundable".equalsIgnoreCase(policyResponse.getRefundPolicyText())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 房型不可退订: {}", policyResponse.getRefundPolicyText());
            }

            // 6. 价格对比验证（必须相等）
            BigDecimal searchTotalPrice = targetHotel.getTotalCharges();
            BigDecimal policyTotalPrice = policyResponse.getTotalBookingAmount();
            
            if (!validatePriceConsistency(searchTotalPrice, policyTotalPrice)) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 价格不一致 - 搜索价格: {}, 取消规则价格: {}", 
                           searchTotalPrice, policyTotalPrice);
                throw new IllegalStateException("价格发生变化，搜索价格: " + searchTotalPrice + ", 最新价格: " + policyTotalPrice);
            }

            // 7. 找到匹配的房型（input.getRoomId() = room.getRoomId()）
            XRoom matchedRoom = xRooms.stream().parallel()
                    .filter(r -> input.getRoomId().equals(r.getRoomId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("未找到匹配的房型: " + input.getRoomId()));


            // 8. 生成createKey
            String createKey = HeyUtil.generateCreateKey(
                getSafeSupplierName(),
                input.getHotelId(),
                input.getRoomId(),
                input.getCheckInDate().toString(),
                input.getCheckOutDate().toString(),
                policyTotalPrice.toString()
            );

            // 9. 构建成功响应
            XOrderCheckResponse response = new XOrderCheckResponse();
            response.setRoom(matchedRoom);
            response.setCreateKey(createKey);
            response.setTotalBasePrice(policyTotalPrice.toString());
            
            logger.info("[AsianOverlandAdapter.orderCheck] 订单校验成功 - 酒店: {}, 房型: {}, 价格: {}, createKey: {}", 
                       input.getHotelId(), input.getRoomId(), policyTotalPrice, createKey);

            return response;

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.orderCheck] 订单校验失败", e);
            throw new RuntimeException("订单校验失败: " + e.getMessage());
        }
    }

    /**
     * 订单前置校验（供应商原始格式）
     * 返回供应商原始校验数据，用于调试或特殊业务场景
     */
    @Override
    public Object orderCheckOrg(XSupplierCheckRequest input) {
        logger.info("[AsianOverlandAdapter.orderCheckOrg] 执行原始格式订单校验, input={}", input);

        try {
            Map<String, Object> result = new HashMap<>();
            
            // 1. 复用orderCheck的搜索逻辑 - 组装 QTechSearchRequest
            QTechSearchRequest req = new QTechSearchRequest();

            // 日期格式转换 yyyy-MM-dd -> dd/MM/yyyy
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.orderCheckOrg] 输入缺少酒店ID，无法报价");
                return result;
            }
            
            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            //从当前酒店详细里获取 : 目的地国家/目的地城市/国籍/居住国
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), input.getHotelId())
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country = hotel.getCountryCode();
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        }
                    });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);

            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索接口
            QTechSearchResponse searchResponse = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheckOrg] 酒店搜索失败", e))
                    .block();

            // 3. 如果有搜索响应，添加到结果中
            if (searchResponse != null) {
                result.put("searchResponse", searchResponse);
                logger.info("[AsianOverlandAdapter.orderCheckOrg] 酒店搜索响应已获取: message={}", searchResponse.getMessage());
                
                // 4. 如果搜索响应成功并获得了searchUniqueId，继续调用取消规则接口
                if ("success".equalsIgnoreCase(searchResponse.getMessage()) && 
                    StrUtil.isNotBlank(searchResponse.getSearchUniqueId())) {
                    
                    String searchUniqueId = searchResponse.getSearchUniqueId();
                    logger.info("[AsianOverlandAdapter.orderCheckOrg] 获取到searchUniqueId: {}, 继续调用取消规则接口", searchUniqueId);
                    
                    // 调用取消规则接口
                    QTechCancellationPolicyRequest policyRequest = new QTechCancellationPolicyRequest();
                    policyRequest.setHotelId(input.getHotelId());
                    policyRequest.setUniqueId(searchUniqueId);
                    policyRequest.setSectionUniqueId(input.getRoomId());
                    
                    QTechCancellationPolicyResponse policyResponse = getCancellationPolicy(policyRequest)
                            .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheckOrg] 获取取消规则失败", e))
                            .block();
                    
                    // 5. 如果有取消规则响应，添加到结果中
                    if (policyResponse != null) {
                        result.put("policyResponse", policyResponse);
                        logger.info("[AsianOverlandAdapter.orderCheckOrg] 取消规则响应已获取: message={}", policyResponse.getMessage());
                    } else {
                        logger.warn("[AsianOverlandAdapter.orderCheckOrg] 取消规则接口无响应");
                    }
                } else {
                    logger.warn("[AsianOverlandAdapter.orderCheckOrg] 搜索响应不成功或无searchUniqueId，跳过取消规则接口调用");
                }
            } else {
                logger.warn("[AsianOverlandAdapter.orderCheckOrg] 酒店搜索接口无响应");
            }

            logger.info("[AsianOverlandAdapter.orderCheckOrg] 原始格式订单校验完成，返回响应数量: {}", result.size());
            return result;

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.orderCheckOrg] 原始格式订单校验失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }


    // ============================================ 报价与订单 ============================================


    /**
     * 获取供应商酒店房型基础信息(原文)
     *
     * @param supplierType
     * @param hotelId
     * @param language
     * @param ext
     * @return
     */
    @Override
    public Object getHotelRoomOrigContent(String supplierType, String hotelId, String language, String ext) {
        logger.info("[AsianOverlandAdapter.getHotelRoomOrigContent] 获取供应商酒店房型基础信息(原文), supplierType={}, hotelId={}, language={}, ext={}", supplierType, hotelId, language, ext);

        try {
            // 1. 组装 QTechSearchRequest
            QTechSearchRequest req = new QTechSearchRequest();

            // 基础认证配置
            SupplierAuth authConfig = extractFromAuthConfig(getSafeSupplierName());
            req.setUsername(authConfig.getUsername());
            req.setPassword(authConfig.getPassword());

            //一周后某个工作日入住1天
            LocalDate checkIn = HeyUtil.nextWeekday(LocalDate.now().plusDays(7));
            LocalDate checkOut = checkIn.plusDays(1);
            // 日期格式转换
            req.setCheckinDate(checkIn.format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(checkOut.format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数（这里只有单个hotelId参数）
            req.setHotelIds(hotelId);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.getHotelRoomOrigContent] 缺少酒店ID");
                throw new IllegalArgumentException("缺少酒店ID");
            }

            // 币种，默认 USD
            req.setSelCurrency("USD");

            // 设置国家信息（简化处理，使用固定值）
            String country = "1"; // 测试用国家代码
            req.setSelNationality(country);
            req.setCountryOfResidence(country);

            // 房间明细
            req.setRoomDetails(HeyUtil.buildQTechRoomDetails("2"));

            // 调用 QTECH 搜索
            QTechSearchResponse response = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getHotelRoomOrigContent] 酒店搜索失败", e))
                    .block();


            if (response != null && response.getStatus() != null && response.getStatus().equalsIgnoreCase("Success")) {
                logger.info("[AsianOverlandAdapter.getHotelRoomOrigContent] 成功获取原始报价数据");
                if (StrUtil.isBlank(response.getSearchUniqueId())) {
                    return response;
                }

                // 2. 获取酒店详情
                QTechHotelDetailRequest detailRequest = new QTechHotelDetailRequest();
                detailRequest.setHotelId(hotelId);
                detailRequest.setUniqueId(response.getSearchUniqueId());

                QTechHotelDetailResponse detailResponse = this.getHotelDetail(detailRequest)
                        .doOnError(e -> logger.error("[AsianOverlandAdapter.getHotelRoomOrigContent] 获取酒店详情失败", e))
                        .block();

                return detailResponse; // 返回原始响应对象
            } else {
                logger.warn("[AsianOverlandAdapter.getHotelRoomOrigContent] 供应商返回失败状态: {}",
                        response != null ? response.getStatus() : "null");
                return response;
            }

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.getHotelRoomOrigContent] 获取原始报价失败", e);
            throw new RuntimeException("获取原始数据失败: " + e.getMessage());
        }
    }


    // ============================================ 工具方法 ==============================================

    /**
     * 验证价格一致性
     * 对比搜索结果价格与取消规则接口价格，必须完全相等
     * 
     * @param searchPrice 搜索结果价格（BigDecimal格式）
     * @param policyPrice 取消规则接口价格（BigDecimal格式）
     * @return true-价格一致，false-价格不一致
     */
    private boolean validatePriceConsistency(BigDecimal searchPrice, BigDecimal policyPrice) {
        try {
            if (searchPrice == null || policyPrice == null) {
                logger.warn("[AsianOverlandAdapter.validatePriceConsistency] 价格参数为空: searchPrice={}, policyPrice={}", 
                           searchPrice, policyPrice);
                return false;
            }
            
            // 使用BigDecimal的compareTo方法进行精确比较（必须完全相等）
            boolean isEqual = searchPrice.compareTo(policyPrice) == 0;
            
            logger.debug("[AsianOverlandAdapter.validatePriceConsistency] 价格对比结果: 搜索价格={}, 取消规则价格={}, 是否相等={}", 
                        searchPrice, policyPrice, isEqual);
            
            return isEqual;
            
        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.validatePriceConsistency] 价格对比异常", e);
            return false;
        }
    }

    /**
     * 将单个酒店的QTech响应数据转换为XRoom列表
     * 复用单酒店报价的转换逻辑，用于多酒店报价场景
     *
     * @param hotel QTech酒店响应数据
     * @param input 原始请求参数
     * @return 转换后的房型列表
     */
    private List<XRoom> convertHotelToXRooms(QTechSearchResponse.Hotel hotel, XSupplierPriceRequest input) {
        List<XRoom> xRooms = new ArrayList<>();

        try {
            List<QTechSearchResponse.HotelProperty> properties = hotel.getHotelProperty();
            logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 处理酒店: ID={}, 名称={},总价={}",
                    hotel.getHotelId(), hotel.getHotelName(), hotel.getTotalCharges());

            if (properties == null || properties.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无房型属性数据", hotel.getHotelId());
                return xRooms;
            }

            Optional<QTechSearchResponse.HotelProperty> targetHotelProp = properties.stream()
                    .filter(p -> p.getType().equals("Selection"))
                    .findFirst();

            if (targetHotelProp.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无Selection类型属性", hotel.getHotelId());
                return xRooms;
            }

            QTechSearchResponse.HotelProperty prop = targetHotelProp.get();
            logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 酒店属性: 星级={}, 地址={}",
                    prop.getDisplayRoomRate(), prop.getType());

            List<QTechSearchResponse.RoomRate> roomRates = prop.getRoomRates();
            if (roomRates == null || roomRates.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无房型报价数据", hotel.getHotelId());
                return xRooms;
            }

            for (QTechSearchResponse.RoomRate roomRate : roomRates) {
                logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 处理房型: ID={}, 类型={}, 餐型={}, 价格={}, 状态={}",
                        roomRate.getClassUniqueId(), roomRate.getRoomType(), roomRate.getMealBasis(),
                        roomRate.getRoomRate(), roomRate.getAvailable());

                XRoom xRoom = new XRoom();
                xRoom.setRoomId(prop.getSectionUniqueId());
                xRoom.setRoomName(roomRate.getRoomType());
                xRoom.setRoomNameEn(roomRate.getRoomType());
                xRoom.setBedTypeDescEn(roomRate.getRoomCategory());

                // 禁烟
                if (roomRate.getRoomType().contains("Non Smoking") || roomRate.getRoomType().contains("Smoking") ||
                        roomRate.getRoomCategory().contains("Non Smoking") || roomRate.getRoomCategory().contains("Smoking")) {
                    xRoom.setNoSmoking(XEnumNoSmoking.NON_SMOKING);
                }

                List<XRatePlan> ratePlans = new ArrayList<>();
                XRatePlan ratePlan = new XRatePlan();
                ratePlan.setRatePlanId(roomRate.getClassUniqueId());
                ratePlan.setRatePlanName(roomRate.getRoomType());
                ratePlan.setDescription(roomRate.getRoomType());
                ratePlan.setAvailable(roomRate.getAvailable());
                ratePlan.setBedTypeDescEn(roomRate.getRoomCategory());

                //货币种类
                ratePlan.setCurrency(HeyUtil.toXwCurrency(hotel.getRateCurrencyCode()).orElse(XEnumCurrency.USD));

                // 是否带餐食
                if (roomRate.getMealBasis().contains("breakfast") || roomRate.getRoomType().contains("breakfast")) {
                    ratePlan.setBreakfast(1);
                }
                if (roomRate.getMealBasis().contains("lunch") || roomRate.getRoomType().contains("lunch")) {
                    ratePlan.setLunch(1);
                }
                if (roomRate.getMealBasis().contains("dinner") || roomRate.getRoomType().contains("dinner")) {
                    ratePlan.setDinner(1);
                }

                ratePlan.setInstantConfirm(false);  //需要调用取消规则接口确费后才能 立即预定

                QTechSearchResponse.Policies policies = prop.getPolicies();
                if (policies == null) {
                    logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无取消规则数据", hotel.getHotelId());
                    continue;
                }

                List<QTechSearchResponse.CancellationPolicy> cancellationPolicy = policies.getCancellationPolicy();
                if (cancellationPolicy == null || cancellationPolicy.isEmpty()) {
                    logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无取消规则列表数据", hotel.getHotelId());
                    continue;
                }

                //是否可退款
                ratePlan.setCancelable(prop.getRefundable());

                //取消规则
                List<XRatePlan.XCancelRule> xCancelRules = new ArrayList<>();
                cancellationPolicy.forEach(policy -> {
                    XRatePlan.XCancelRule xCancelRule = new XRatePlan.XCancelRule();
                    xCancelRule.setStartTimeOrig(policy.getStart().toString());
                    xCancelRule.setStartTimeOrig(policy.getEnd().toString());
                    xCancelRule.setStartTime(policy.getStart().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
                    xCancelRule.setEndTime(policy.getEnd().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
                    xCancelRule.setIsAfter(true);
                    xCancelRule.setDeductValue(String.valueOf(policy.getCharges()));
                    xCancelRules.add(xCancelRule);
                });
                //设置取消规则
                ratePlan.setCancelRules(xCancelRules);

                //设置 房型 单间的价格
                ratePlan.setPrice(String.valueOf(roomRate.getRoomRate()));
                ratePlan.setBasePrice(String.valueOf(roomRate.getRoomRate()));

                //设置 日价明细
                List<QTechSearchResponse.RateBreakup> rateBreakups = roomRate.getRateBreakup();
                List<XRatePlanDaily> dailyPrices = new ArrayList<>();
                rateBreakups.forEach(breakup -> {
                    XRatePlanDaily daily = new XRatePlanDaily();
                    daily.setAvailable(1);
                    daily.setDate(breakup.getDate());
                    daily.setPrice(String.valueOf(breakup.getDisplayNightlyRate()));
                    daily.setBasePrice(String.valueOf(breakup.getDisplayNightlyRate()));
                    daily.setMemberPrice(String.valueOf(breakup.getDisplayNightlyRate()));
                    daily.setQuantity(1);
                    daily.setCurrency(HeyUtil.toXwCurrency(hotel.getRateCurrencyCode()).orElse(XEnumCurrency.USD));
                    daily.setCancelable(prop.getRefundable());
                    daily.setInstantConfirm(false);
                    dailyPrices.add(daily);
                });
                ratePlan.setDailys(dailyPrices);

                //设置 入住信息
                ratePlan.setCheckInDate(input.getCheckInDate());
                ratePlan.setCheckOutDate(input.getCheckOutDate());
                ratePlan.setQuantity(input.getRoomNum());

                //添加到房价列表
                ratePlans.add(ratePlan);

                //设置 房型价格
                xRoom.setRatePlans(ratePlans);
                //设置 酒店报价
                xRoom.setMinPrice(hotel.getTotalCharges());
                xRoom.setMinBasePrice(hotel.getTotalCharges());
                //添加到房型列表
                xRooms.add(xRoom);

                logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 转换报价: {}", xRoom);
            }

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.convertHotelToXRooms] 转换酒店{}数据失败", hotel.getHotelId(), e);
        }

        return xRooms;
    }

    /**
     * 合并酒店ID参数，兼容单酒店和多酒店场景
     *
     * @param hotelId  单酒店ID
     * @param hotelIds 多酒店ID集合（用分隔符分隔）
     * @return 逗号分隔的酒店ID字符串（最多100个）
     */
    private String mergeHotelIds(String hotelId, String hotelIds) {
        Set<String> idSet = new LinkedHashSet<>(); // 使用LinkedHashSet保持顺序并去重

        // 处理单酒店ID
        if (!StrUtil.isBlank(hotelId)) {
            idSet.add(hotelId.trim());
        }

        // 处理多酒店ID集合
        if (!StrUtil.isBlank(hotelIds)) {
            // 支持多种分隔符：逗号、竖线、连字符
            String[] ids = hotelIds.split("[,|\\-]+");
            for (String id : ids) {
                if (!StrUtil.isBlank(id)) {
                    idSet.add(id.trim());
                }
            }
        }

        // 限制最多100个酒店ID
        if (idSet.size() > 100) {
            logger.warn("[AsianOverlandAdapter.mergeHotelIds] 酒店ID数量超过限制，截取前100个，原数量：{}", idSet.size());
            idSet = idSet.stream().limit(100).collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        }

        String result = String.join(",", idSet);
        logger.debug("[AsianOverlandAdapter.mergeHotelIds] 合并后的酒店ID：{}，数量：{}", result, idSet.size());

        return result;
    }

    /**
     * 将 yyyy-MM-dd 转换为 dd/MM/yyyy；若解析失败，原样返回
     */
    private String formatToQtechDate(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isEmpty()) return yyyyMMdd;
        try {
            LocalDate d = LocalDate.parse(yyyyMMdd);
            return d.format(HeyUtil.DATE_FORMATTER_DDMMYYYY);
        } catch (DateTimeParseException e) {
            logger.warn("日期格式解析失败(期望yyyy-MM-dd): {}", yyyyMMdd);
            return yyyyMMdd;
        }
    }


}
