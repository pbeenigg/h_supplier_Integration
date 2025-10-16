package com.heytrip.hotel.supplier.adapter.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.heytrip.common.enums.*;
import com.heytrip.common.request.XCancelOrderRequest;
import com.heytrip.common.request.XCreateOrderRequest;
import com.heytrip.common.request.XSupplierCheckRequest;
import com.heytrip.common.request.XSupplierPriceRequest;
import com.heytrip.common.response.base.XRatePlan;
import com.heytrip.common.response.base.XRatePlanDaily;
import com.heytrip.common.response.base.XRoom;
import com.heytrip.common.response.other.XCancelOrderResponse;
import com.heytrip.common.response.other.XCreateOrderResponse;
import com.heytrip.common.response.other.XOrderCheckResponse;
import com.heytrip.common.response.other.XQueryOrderResponse;
import com.heytrip.hotel.supplier.adapter.AbstractSupplierAdapter;
import com.heytrip.hotel.supplier.adapter.builder.QTechQueryBuilder;
import com.heytrip.hotel.supplier.adapter.capability.OrderBridge;
import com.heytrip.hotel.supplier.adapter.capability.PricingBridge;
import com.heytrip.hotel.supplier.adapter.capability.StaticBridge;
import com.heytrip.hotel.supplier.adapter.service.StaticDataQueryService;
import com.heytrip.hotel.supplier.client.HttpClientService;
import com.heytrip.hotel.supplier.dto.qtech.req.*;
import com.heytrip.hotel.supplier.dto.qtech.resp.*;
import com.heytrip.hotel.supplier.dto.supplier.SupplierAuth;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.enums.QTechBookingStatusEnum;
import com.heytrip.hotel.supplier.exception.SupplierException;
import com.heytrip.hotel.supplier.utils.HeyUtil;
import com.heytrip.hotel.supplier.utils.MD5Util;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

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
                throw SupplierException.invalidParameter(getSafeSupplierName(),"数据校验失败: " + msg);
            }

            logger.debug("调用QTECH搜索API，目的地: {}, 入住: {}, 离店: {}",
                    request.getSelCity(), request.getCheckinDate(), request.getCheckoutDate());

            String endpoint = QTechQueryBuilder.buildEndpoint(request);
            logger.debug("构建的搜索端点: {}", endpoint);

            return executeGetRequest(SEARCH_BASE_URL, endpoint, QTechSearchResponse.class);

        } catch (Exception e) {
            logger.error("QTECH搜索请求失败", e);
            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"搜索请求失败: " + e.getMessage()));
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
                        if (policy == null || !"success".equalsIgnoreCase(policy.getMessage())) {
                            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"获取取消规则失败,无法进行预定"));
                        }

                        if (policy == null || policy.getTotalBookingAmount() == null) {
                            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"获取取消规则失败,无法获取预定价格"));
                        }

                        // 再次验证预定价格与取消规则中的价格一致
                       verifyBookingPrice(request.getExpectedPrice(), policy.getTotalBookingAmount());


                        QTechCancellationPolicyResponse.BookingAllowedInfo allowedInfo = policy.getBookingAllowedInfo();
                        if (allowedInfo == null || !"yes".equalsIgnoreCase(allowedInfo.getBookingAllowed())) {
                            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"当前房型不可预订"));
                        }

                        // 2. 执行预订
                        return executeReservation(request, policy);
                    })
                    .doOnSuccess(result -> logger.info("QTECH预订完成，状态: {}",
                            result != null ? result.getMessage() : "未知"))
                    .doOnError(error -> logger.error("QTECH预订失败: {}", error.getMessage()));

        } catch (Exception e) {
            logger.error("QTECH预订请求构建失败", e);
            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"预订请求构建失败: " + e.getMessage()));
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
                    .doOnError(e -> logger.error("QTECH酒店详情获取失败，酒店ID:{}  Error:{}", request.getHotelId(), e.getMessage()));

        } catch (Exception e) {
            logger.error("QTECH酒店详情请求构建失败", e);
            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"酒店详情请求构建失败: " + e.getMessage()));
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
                    .doOnError(e -> Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"QTECH预订详情获取失败，预订ID: " + request.getBookingId() + "  Error:" + e.getMessage())));

        } catch (Exception e) {
            logger.error("QTECH预订详情请求构建失败", e);
            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"预订详情请求构建失败: " + e.getMessage()));
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
                        if (chargesResult == null || !"success".equalsIgnoreCase(chargesResult.getMessage())) {
                            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"获取取消费用失败"));
                        }

                        // 2. 执行取消
                        return executeCancellation(request);
                    })
                    .doOnSuccess(result -> logger.info("QTECH取消完成，状态: {}", result != null ? result.getMessage() : "未知"))
                    .doOnError(e -> logger.error("QTECH取消失败:{}", e.getMessage()));

        } catch (Exception e) {
            logger.error("QTECH取消请求构建失败", e);
            return Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"取消请求构建失败: " + e.getMessage()));
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

            if (input.getCheckInDate() == null || input.getCheckOutDate() == null) {
                throw SupplierException.missingParameter(getSafeSupplierName(),"缺少入住或离店日期");
            }

            // 日期格式转换 yyyy-MM-dd -> dd/MM/yyyy
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.getPrice] 输入缺少酒店ID，无法报价");
                throw SupplierException.missingParameter(getSafeSupplierName(),"输入缺少酒店ID，无法报价");
            }
            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            //从当前酒店详细里获取 : 目的地国家/目的地城市/国籍/居住国
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), input.getHotelId())
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country = String.valueOf(hotel.getCountryId());
                            //String country = "138"; //TODO  测试
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        } else {
                            throw SupplierException.invalidParameter(getSafeSupplierName(),"酒店ID无效，无法获取酒店信息");
                        }
                    });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);
            if (details.size() != input.getRoomNum()) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数与入住信息不匹配");
            }
            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索
            QTechSearchResponse resp = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getPrice] 酒店搜索失败:{}", e.getMessage()))
                    .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getPrice] 酒店搜索失败:" + e.getMessage()))
                    .block();

            if (resp == null) {
                logger.warn("[AsianOverlandAdapter.getPrice] QTECH无响应，返回空结果");
                throw SupplierException.invalidParameter(getSafeSupplierName(),"酒店搜索失败无响应");
            }
            if (!"success".equalsIgnoreCase(resp.getMessage())) {
                logger.warn("[AsianOverlandAdapter.getPrice] QTECH返回非成功: message={}, info={}", resp.getMessage(), resp.getMessageInfo());
                throw SupplierException.invalidParameter(getSafeSupplierName(),"酒店搜索失败:" + resp.getMessage());
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
                logger.error("[AsianOverlandAdapter.getPrice] 返回酒店列表中不包含请求的酒店ID: {}", input.getHotelId());
                return Collections.emptyList();
            }

            // 使用共用的转换方法
            QTechSearchResponse.Hotel targetHotel = targetHotelOpt.get();
            List<XRoom> xRooms = convertHotelToXRooms(targetHotel, input.getCheckInDate(), input.getCheckOutDate(), input.getRoomNum());

            if (xRooms.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.getPrice] 酒店{}转换后无有效房型数据", targetHotel.getHotelId());
                return Collections.emptyList();
            } else {
                logger.info("[AsianOverlandAdapter.getPrice] 单酒店报价完成，酒店{}返回{}个房型",
                        targetHotel.getHotelId(), xRooms.size());
            }

            // 过滤指定房型ID（如果有提供）
            if (StrUtil.isBlank(input.getRoomId())) {
                return xRooms;
            } else {
                return xRooms.stream().filter(r -> r.getRoomId().equalsIgnoreCase(input.getRoomId())).collect(Collectors.toList());

            }
        } catch (Exception ex) {
            logger.error("[AsianOverlandAdapter.getPrice] 获取报价失败", ex);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"获取报价失败: " + ex.getMessage());
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

            if (input.getCheckInDate() == null || input.getCheckOutDate() == null) {
                throw SupplierException.missingParameter(getSafeSupplierName(),"缺少入住或离店日期");
            }

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                throw SupplierException.missingParameter(getSafeSupplierName(),"输入缺少酒店ID，无法报价");
            }

            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            //尝试从第一个酒店获取国家代码
            Arrays.stream(hotelIds.split(",")).findFirst().ifPresent(firstHotelId -> {
                staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), firstHotelId)
                        .ifPresent(hotel -> {
                            if (hotel != null && ObjUtil.isNotNull(hotel.getCountryId())) {
                                req.setSelNationality(String.valueOf(hotel.getCountryId()));
                                req.setCountryOfResidence(String.valueOf(hotel.getCountryId()));
                            }
                        });
            });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);
            if (details.size() != input.getRoomNum()) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数与入住信息不匹配");
            }
            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索
            QTechSearchResponse response = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getPrices] 酒店搜索失败:{}", e.getMessage()))
                    .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getPrices] 酒店搜索失败:" + e.getMessage()))
                    .block();

            if (response == null || !"success".equalsIgnoreCase(response.getMessage())) {
                logger.warn("[AsianOverlandAdapter.getPrices] QTECH无响应，返回空结果");
                throw SupplierException.invalidParameter(getSafeSupplierName(),"酒店搜索失败无响应或返回非成功");
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
                List<XRoom> xRooms = convertHotelToXRooms(hotel, input.getCheckInDate(), input.getCheckOutDate(), input.getRoomNum());

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
            throw SupplierException.invalidParameter(getSafeSupplierName(),"获取原始报价失败: " + e.getMessage());
        }
    }

    /**
     * 创建订单桥接（预定酒店）
     */
    public XCreateOrderResponse createOrder(XCreateOrderRequest input) {
        logger.info("[AsianOverlandAdapter.createOrder] 占位实现, input={}", input);
        /// 需求前提条件：必须先通过 搜索酒店接口获取 到的房型ID（sectionUniqueId）和搜索唯一标识（searchUniqueId）才能预定。
        /// 预订流程：
        ///  1. 使用订单请求参数组装 QTechReservationRequest 对象。
        ///  2. 调用 bookHotel 方法执行预订。
        ///  3. 处理预订响应，转换为 XCreateOrderResponse 格式返回。
        ///  4. 通过搜索酒店接口，可以拿到最新价格，可预定状态，搜索唯一标识（searchUniqueId），房型唯一标识（sectionUniqueId）等信息。  拿到了才能去调用预定酒店接口
        ///  5. 预定成功后，如若预定接口在（3s ~ 10s）还未响应成功， 就异步调用订单详情接口，获取最终的订单状态和信息。 然后预定立即返回给渠道状态 （预定中 ｜预定成功 ｜预定失败）
        ///
        ///使用限制与规则
        /// 1.每笔预订的夜数：最多 30 晚
        /// 2.每笔预订的客人上限：5 间房、10 位客人
        /// 3.儿童年龄：0-12 岁
        /// 4.每间房最多儿童数：3 名
        /// 5.服务日期不应超过未来 365 天


        // 组装 QTechSearchRequest
        QTechSearchRequest searchRequest = new QTechSearchRequest();
        // 日期格式转换
        searchRequest.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
        searchRequest.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

        if (input.getCheckInDate() == null || input.getCheckOutDate() == null) {
            throw SupplierException.missingParameter(getSafeSupplierName(),"缺少入住或离店日期");
        }
        if (!input.getCheckOutDate().isAfter(input.getCheckInDate())) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"离店日期必须晚于入住日期");
        }
        if (input.getCheckOutDate().isAfter(input.getCheckInDate().plusDays(30))) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"每笔预订的夜数不能超过30晚");
        }
        if (input.getCheckInDate().isAfter(LocalDateTime.now().plusDays(365))) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"服务日期不应超过未来365天");
        }
        if (StrUtil.isBlank(input.getDistributorOrderId())) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少分销商订单号");
        }
        if (StrUtil.isBlank(input.getHotelId())) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少酒店ID");
        }
        if (input.getRoomNum() <= 0 || input.getRoomNum() > 5) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数必须在1到5之间");
        }
        if (input.getOccupancy() == null || input.getOccupancy().isEmpty()) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少入住信息");
        }
        if (StrUtil.isBlank(input.getCurrency())) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少币种");
        }

        searchRequest.setHotelIds(input.getHotelId());
        if (StrUtil.isBlank(searchRequest.getHotelIds())) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"输入缺少酒店ID，无法报价");
        }
        // 币种，默认 USD
        searchRequest.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

        // 设置国家信息
        staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), searchRequest.getHotelIds())
                .ifPresent(hotel -> {
                    if (hotel != null) {
                        String country = String.valueOf(hotel.getCountryId());
                        //String country = "138"; //TODO  测试
                        searchRequest.setSelNationality(country);
                        searchRequest.setCountryOfResidence(country);
                    } else {
                        throw SupplierException.invalidParameter(getSafeSupplierName(),"酒店ID无效，无法获取酒店信息");
                    }
                });

        // 房间明细与房间数
        List<QTechSearchRequest.RoomDetail> roomDetails = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
        searchRequest.setRoomDetails(roomDetails);
        searchRequest.setNumberOfRooms(roomDetails != null ? roomDetails.size() : 0);
        if (roomDetails.size() != input.getRoomNum()) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数与入住信息不匹配");
        }
        // 可根据需要设置静态信息、limit、availableonly 等
        searchRequest.setAvailableonly(1);
        searchRequest.setStaticData(1);

        // 2. 调用 QTECH 搜索
        QTechSearchResponse searchResponse = this.searchHotels(searchRequest)
                .doOnError(e -> logger.error("[AsianOverlandAdapter.createOrder] 酒店搜索失败:{}", e.getMessage()))
                .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.createOrder] 酒店搜索失败:" + e.getMessage()))
                .block();

        if (searchResponse != null && searchResponse.getMessage().equalsIgnoreCase("success")) {
            // 组装预订请求
            QTechReservationRequest reservationRequest = new QTechReservationRequest();
            // 这里需要根据 input 构建预订请求对象


            // 仅处理指定酒店ID的报价
            Optional<QTechSearchResponse.Hotel> targetHotelOpt = searchResponse.getHotelList().stream()
                    .filter(h -> input.getHotelId().equals(h.getHotelId()))
                    .findFirst();
            if (targetHotelOpt.isEmpty()) {
                throw SupplierException.notFound(getSafeSupplierName(),"返回酒店列表中不包含请求的酒店ID: " + input.getHotelId());
            }

            // 使用共用的转换方法
            QTechSearchResponse.Hotel targetHotel = targetHotelOpt.get();
            List<XRoom> xRooms = convertHotelToXRooms(targetHotel, input.getCheckInDate(), input.getCheckOutDate(), input.getRoomNum());

            if (xRooms.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.createOrder] 酒店{}转换后无有效房型数据", targetHotel.getHotelId());
                throw SupplierException.notFound(getSafeSupplierName(),"无有效房型数据");
            }
            logger.info("[AsianOverlandAdapter.createOrder] 单酒店报价完成，酒店:{} 返回:{}个房型,总价:{}", targetHotel.getHotelId(), xRooms.size(), targetHotel.getTotalCharges());


            /// 获取搜索唯一标识（后边调用取消规则接口需要用到） 每个搜索唯一ID只能用于一次预订，并且从搜索时间起20分钟内有效
            String searchUniqueId = searchResponse.getSearchUniqueId();
            if (StrUtil.isBlank(input.getRoomId())) {
                throw SupplierException.missingParameter(getSafeSupplierName(),"缺少房型ID");
            }

            // 找到匹配的房型（input.getRoomId() = room.getRoomId()）
            XRoom matchedRoom = xRooms.stream().parallel()
                    .filter(r -> input.getRoomId().equals(r.getRoomId()))
                    .findFirst()
                    .orElseThrow(() -> SupplierException.invalidParameter(getSafeSupplierName(),"未找到匹配的房型: " + input.getRoomId()));

            // 校验币种一致性
            if (!input.getCurrency().equalsIgnoreCase(targetHotel.getRateCurrencyCode())) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"预订币种与报价币种不一致，无法预订");
            }

            //从房型扩展信息里获取 房型唯一标识（sectionUniqueId）和 房间类型ID（classUniqueId）
            QTechSearchResponse.RoomRateExt roomRateExt = JSONUtil.toBean(matchedRoom.getExt(), QTechSearchResponse.RoomRateExt.class);
            if (roomRateExt == null || StrUtil.isBlank(roomRateExt.getSectionUniqueId()) || CollUtil.isEmpty(roomRateExt.getClassUniqueId())) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"所预定的房型扩展信息缺失，无法预定");
            }

            // 设置预定价格 - 实现价格判断和设置逻辑
            BigDecimal finalBookingPrice = verifyBookingPrice(input.getSalePrice(), matchedRoom.getMinBasePrice());
            reservationRequest.setExpectedPrice(finalBookingPrice);
            reservationRequest.setHotelId(input.getHotelId());
            reservationRequest.setAgentRefNo(input.getDistributorOrderId()); // 订单校验返回的 订单唯一号  （分销商系统订单号）
            reservationRequest.setUniqueId(searchUniqueId); //从搜索酒店结果中获取
            reservationRequest.setSectionUniqueId(roomRateExt.getSectionUniqueId()); // 房型唯一标识，每次查询酒店报价的唯一标识（动态变化）

            //// 构建预订房间明细（需要转换为JSON字符串）
            /// 根据 input.getRoomNum() 入参的房间数，构建对应数量的房间明细，如果只有一个房间，则只构建一个
            /// 然后还需要为每个房间设置一个 对应的 房间类型ID（roomClassId）= input.getRatePlanId(), 但是入参只支持一个房间类型 ID
            /// 如果 input.getRoomNum() 入参的房间数 > 1 则表示多间房， 但是没有传递多个房间类型 ID 的参数，暂时只能使用同一个房间类型 ID
            /// 如果需要支持多间房且不同房型，则需要扩展入参，目前先按同一房型处理
            String roomDetailsJson = buildReservationRoomDetails(input, roomDetails, roomRateExt.getClassUniqueId());
            reservationRequest.setRoomDetails(roomDetailsJson);


            // 执行预定流程：预定接口 + 超时处理 + 订单详情轮询
            XCreateOrderResponse orderResponse = executeBookingWithTimeoutAndPolling(reservationRequest, input);

            return orderResponse;


        } else {
            logger.warn("[AsianOverlandAdapter.createOrder] QTECH返回非成功: message={}, info={}", searchResponse != null ? searchResponse.getMessage() : "null", searchResponse != null ? searchResponse.getMessageInfo() : "null");
            throw SupplierException.invalidParameter(getSafeSupplierName(),"预订失败，无法获取预定酒店信息");
        }
    }

    /**
     * 取消订单桥接（取消预定）
     * <p>
     * 流程说明：
     * 1. 先调用取消费用接口，获取取消费用和是否允许取消
     * 2. 如果不允许取消，直接返回失败响应
     * 3. 如果允许取消，调用取消预订接口
     * 4. 根据取消费用和退款金额构建响应
     */
    public XCancelOrderResponse cancelOrder(XCancelOrderRequest input) {
        logger.info("[AsianOverlandAdapter.cancelOrder] 开始取消订单流程, supplierOrderId={}", input.getSupplierOrderId());


        // 1. 获取取消费用
        QTechGetCancellationChargesRequest chargesRequest = new QTechGetCancellationChargesRequest();
        if (StrUtil.isBlank(input.getSupplierOrderId())) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少供应商订单号，无法取消订单");
        }
        chargesRequest.setBookingId(input.getSupplierOrderId());


        QTechCancellationChargesResponse chargesResponse = this.getCancellationCharges(chargesRequest)
                .timeout(Duration.ofSeconds(60), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.cancelOrder] 获取取消费用超时")))
                .doOnError(e -> logger.error("[AsianOverlandAdapter.cancelOrder] 获取取消费用失败:{}", e.getMessage()))
                .block();

        // 2. 检查取消费用响应
        if (chargesResponse == null || !"success".equalsIgnoreCase(chargesResponse.getMessage())) {
            logger.error("[AsianOverlandAdapter.cancelOrder] 获取取消费用失败，响应: {}", chargesResponse);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"取消失败",
                    buildCancelFailedResponse(input.getSupplierOrderId(), "获取取消费用失败"));
        }

        // 3. 检查是否允许取消
        if (!"yes".equalsIgnoreCase(chargesResponse.getAllowCancel())) {
            logger.warn("[AsianOverlandAdapter.cancelOrder] 订单不允许取消，allowCancel: {}, message: {}",
                    chargesResponse.getAllowCancel(), chargesResponse.getMessageInfo());
            throw SupplierException.invalidParameter(getSafeSupplierName(),chargesResponse.getMessageInfo(),
                    buildCancelNotAllowedResponse(input.getSupplierOrderId(), chargesResponse.getMessageInfo()));
        }

        logger.info("[AsianOverlandAdapter.cancelOrder] 订单允许取消，取消费用: {} {}",
                chargesResponse.getCancellationCharge(), chargesResponse.getDisplayCurrencyCode());

        // 4. 执行取消预订
        QTechCancellationBookingRequest cancelRequest = new QTechCancellationBookingRequest();
        cancelRequest.setBookingId(input.getSupplierOrderId());

        QTechCancellationResponse cancellationResponse = this.cancelBooking(cancelRequest)
                .timeout(Duration.ofSeconds(60), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.cancelOrder] 取消预订超时")))
                .doOnError(e -> logger.error("[AsianOverlandAdapter.cancelOrder] 取消预订失败:{}", e.getMessage()))
                .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.cancelOrder] 取消预订失败:" + e.getMessage()))
                .block();

        // 5. 检查取消响应
        if (cancellationResponse == null || !"success".equalsIgnoreCase(cancellationResponse.getMessage())) {
            logger.error("[AsianOverlandAdapter.cancelOrder] 取消预订失败，响应: {}", cancellationResponse);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"取消失败",
                    buildCancelFailedResponse(input.getSupplierOrderId(), "取消预订失败"));
        }

        // 6. 构建成功响应
        return buildCancelSuccessResponse(input.getSupplierOrderId(), chargesResponse, cancellationResponse);


    }

    /**
     * 查询订单桥接（查询预定详情）
     * <p>
     * 流程说明：
     * 1. 调用订单详情接口获取最新的订单状态和信息
     * 2. 将QTECH状态映射为标准订单状态
     * 3. 构建完整的查询订单响应
     */
    public XQueryOrderResponse queryOrder(String distributorOrderId, String supplierOrderId, String ext) {
        logger.info("[AsianOverlandAdapter.queryOrder] 开始查询订单详情, distributorOrderId={}, supplierOrderId={}, ext={}",
                distributorOrderId, supplierOrderId, ext);


        // 1. 调用订单详情接口
        QTechBookingDetailRequest detailRequest = new QTechBookingDetailRequest();
        if (StrUtil.isNotBlank(supplierOrderId)) {
            detailRequest.setBookingId(supplierOrderId);
        } else if (StrUtil.isNotBlank(distributorOrderId)) {
            // 支持通过分销商订单号查询
            detailRequest.setAgentRefNo(distributorOrderId);
        } else {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少供应商订单号和分销商订单号，无法查询订单");
        }

        QTechBookingDetailResponse detailResponse = this.getBookingDetail(detailRequest)
                .timeout(Duration.ofSeconds(60))
                .doOnError(e -> logger.error("[AsianOverlandAdapter.queryOrder] 获取预订详情失败:{}", e.getMessage()))
                .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.queryOrder] 获取预订详情失败:" + e.getMessage()))
                .block();

        // 2. 检查响应
        if (detailResponse == null || !"success".equalsIgnoreCase(detailResponse.getMessage())) {
            logger.error("[AsianOverlandAdapter.queryOrder] 获取订单详情失败，响应: {}", detailResponse);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"查询失败"
                    , buildQueryFailedResponse(distributorOrderId, supplierOrderId, "获取订单详情失败"));
        }

        // 3. 构建查询响应
        return buildQuerySuccessResponse(distributorOrderId, supplierOrderId, detailResponse);


    }

    /**
     * 获取原始单酒店报价（供应商原始数据格式）
     * 返回 QTech 供应商的原始搜索响应数据
     */
    @Override
    public Object getPriceOrig(XSupplierPriceRequest input) {
        logger.info("[AsianOverlandAdapter.getPriceOrig] 获取原始报价数据, input={}", input);


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
            throw SupplierException.invalidParameter(getSafeSupplierName(),"输入缺少酒店ID，无法报价");
        }

        // 币种，默认 USD
        req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

        //尝试从第一个酒店获取国家代码
        Arrays.stream(hotelIds.split(",")).findFirst().ifPresent(firstHotelId -> {
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), firstHotelId)
                    .ifPresent(hotel -> {
                        if (hotel != null && ObjUtil.isNotNull(hotel.getCountryId())) {
                            req.setSelNationality(String.valueOf(hotel.getCountryId()));
                            req.setCountryOfResidence(String.valueOf(hotel.getCountryId()));
                        }
                    });
        });

        // 房间明细与房间数
        List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
        req.setRoomDetails(details);
        req.setNumberOfRooms(details != null ? details.size() : 0);
        if (details.size() != input.getRoomNum()) {
            throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数与入住信息不匹配");
        }
        // 可根据需要设置静态信息、limit、availableonly 等
        req.setAvailableonly(1);
        req.setStaticData(1);

        // 2. 调用 QTECH 搜索
        QTechSearchResponse response = this.searchHotels(req)
                .doOnError(e -> logger.error("[AsianOverlandAdapter.getPrice] 酒店搜索失败:{}", e.getMessage()))
                .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getPriceOrig] 获取原始报价失败:" + e.getMessage()))
                .timeout(Duration.ofSeconds(90), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getPriceOrig] 获取原始报价超时")))
                .block();

        if (response != null && response.getMessage().equalsIgnoreCase("success")) {
            logger.info("[AsianOverlandAdapter.getPriceOrig] 成功获取原始报价数据");
            return response; // 返回原始响应对象
        } else {
            logger.warn("[AsianOverlandAdapter.getPriceOrig] 供应商返回失败状态: {}",
                    response != null ? response.getMessage() : "null");
            return response;
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

            if (input.getCheckInDate() == null || input.getCheckOutDate() == null) {
                throw SupplierException.missingParameter(getSafeSupplierName(),"缺少入住或离店日期");
            }
            if (!input.getCheckOutDate().isAfter(input.getCheckInDate())) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"离店日期必须晚于入住日期");
            }
            // 日期格式转换 yyyy-MM-dd -> dd/MM/yyyy
            req.setCheckinDate(input.getCheckInDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));
            req.setCheckoutDate(input.getCheckOutDate().format(HeyUtil.DATE_FORMATTER_DDMMYYYY));

            // 酒店ID - 兼容单酒店和多酒店参数
            String hotelIds = mergeHotelIds(input.getHotelId(), input.getHotelIds());
            req.setHotelIds(hotelIds);
            if (StrUtil.isBlank(req.getHotelIds())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 输入缺少酒店ID，无法报价");
                throw SupplierException.invalidParameter(getSafeSupplierName(),"输入缺少酒店ID");
            }

            // 币种，默认 USD
            req.setSelCurrency(StrUtil.isBlank(input.getCurrency()) ? "USD" : input.getCurrency());

            //从当前酒店详细里获取 : 目的地国家/目的地城市/国籍/居住国
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), input.getHotelId())
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country = String.valueOf(hotel.getCountryId());
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        }
                    });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);
            if (details.size() != input.getRoomNum()) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数与入住信息不匹配");
            }
            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索
            QTechSearchResponse searchResponse = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheck] 酒店搜索失败:{}", e.getMessage()))
                    .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.orderCheck] 酒店搜索失败:" + e.getMessage()))
                    .block();

            if (searchResponse == null) {
                logger.warn("[AsianOverlandAdapter.orderCheck] QTECH无响应，返回空结果");
                throw SupplierException.invalidParameter(getSafeSupplierName(),"QTECH接口无响应");
            }
            if (!"success".equalsIgnoreCase(searchResponse.getMessage())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] QTECH返回非成功: message={}, info={}", searchResponse.getMessage(), searchResponse.getMessageInfo());
                throw SupplierException.invalidParameter(getSafeSupplierName(),"QTECH返回非成功: " + searchResponse.getMessage());
            }

            List<QTechSearchResponse.Hotel> hotelList = searchResponse.getHotelList();
            if (hotelList == null || hotelList.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 返回成功但无酒店数据");
                throw SupplierException.invalidParameter(getSafeSupplierName(),"返回成功但无酒店数据");
            }

            // 仅处理指定酒店ID的报价
            Optional<QTechSearchResponse.Hotel> targetHotelOpt = hotelList.stream()
                    .filter(h -> input.getHotelId().equals(h.getHotelId()))
                    .findFirst();
            if (targetHotelOpt.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 返回酒店列表中不包含请求的酒店ID: {}", input.getHotelId());
                throw SupplierException.notFound(getSafeSupplierName(),"返回酒店列表中不包含请求的酒店ID: " + input.getHotelId());
            }

            // 使用共用的转换方法
            QTechSearchResponse.Hotel targetHotel = targetHotelOpt.get();
            List<XRoom> xRooms = convertHotelToXRooms(targetHotel, input.getCheckInDate(), input.getCheckOutDate(), input.getRoomNum());

            if (xRooms.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 酒店{}转换后无有效房型数据", targetHotel.getHotelId());
                throw SupplierException.notFound(getSafeSupplierName(),"无有效房型数据");
            }
            logger.info("[AsianOverlandAdapter.orderCheck] 单酒店报价完成，酒店:{} 返回:{}个房型,总价:{}", targetHotel.getHotelId(), xRooms.size(), targetHotel.getTotalCharges());


            /// 获取搜索唯一标识（后边调用取消规则接口需要用到） 每个搜索唯一ID只能用于一次预订，并且从搜索时间起20分钟内有效
            String searchUniqueId = searchResponse.getSearchUniqueId();
            if (StrUtil.isBlank(input.getRoomId())) {
                throw SupplierException.missingParameter(getSafeSupplierName(),"缺少房型ID");
            }

            // 找到匹配的房型（input.getRoomId() = room.getRoomId()）
            XRoom matchedRoom = xRooms.stream().parallel()
                    .filter(r -> input.getRoomId().equals(r.getRoomId()))
                    .findFirst()
                    .orElseThrow(() -> SupplierException.invalidParameter(getSafeSupplierName(),"未找到匹配的房型: " + input.getRoomId()));

            //从房型扩展信息里获取 房型唯一标识（sectionUniqueId）和 房间类型ID（classUniqueId）
            QTechSearchResponse.RoomRateExt roomRateExt = JSONUtil.toBean(matchedRoom.getExt(), QTechSearchResponse.RoomRateExt.class);
            if (roomRateExt == null || StrUtil.isBlank(roomRateExt.getSectionUniqueId()) || CollUtil.isEmpty(roomRateExt.getClassUniqueId())) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"所预定的房型扩展信息缺失，无法预定");
            }

            // 3. 调用取消规则接口获取最新的预定价格和取消规则
            QTechCancellationPolicyRequest policyRequest = new QTechCancellationPolicyRequest();
            policyRequest.setHotelId(targetHotel.getHotelId());
            policyRequest.setUniqueId(searchUniqueId);
            policyRequest.setSectionUniqueId(roomRateExt.getSectionUniqueId()); // 房型唯一标识，每次查询酒店报价的唯一标识（动态变化）

            QTechCancellationPolicyResponse policyResponse = getCancellationPolicy(policyRequest)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheck] 获取酒店取消规则失败:" + e.getMessage()))
                    .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.orderCheck] 获取酒店取消规则失败:" + e.getMessage()))
                    .block();

            if (policyResponse == null) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 取消规则接口无响应");
                throw SupplierException.invalidParameter(getSafeSupplierName(),"取消规则接口无响应");
            }
            if (!"success".equalsIgnoreCase(policyResponse.getMessage())) {
                logger.warn("[AsianOverlandAdapter.orderCheck] 取消规则接口返回非成功: message={}, info={}",
                        policyResponse.getMessage(), policyResponse.getMessageInfo());
                throw SupplierException.invalidParameter(getSafeSupplierName(),"取消规则接口返回非成功: " + policyResponse.getMessage());
            }

            // 4. 验证预订允许状态
            QTechCancellationPolicyResponse.BookingAllowedInfo bookingInfo = policyResponse.getBookingAllowedInfo();
            if (bookingInfo == null || !"yes".equalsIgnoreCase(bookingInfo.getBookingAllowed())) {
                String reason = bookingInfo != null ? bookingInfo.getMessage() : "未知原因";
                logger.warn("[AsianOverlandAdapter.orderCheck] 房型不允许预订: {}", reason);
                throw SupplierException.invalidParameter(getSafeSupplierName(),"房型不允许预订: " + reason);
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

                ///  后续可以根据业务需求调整 目前改为不阻断验单查询，  直接返回最新价格给渠道，由渠道决定是否继续预订
                ///  后期还可以在这里做个价格变动的监控统计， 方便统计出价格波动较大的酒店
               // throw SupplierException.invalidParameter(getSafeSupplierName(),"价格发生变化，搜索价格: " + searchTotalPrice + ", 最新价格: " + policyTotalPrice);
            }

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
            response.setTotalPrice(policyTotalPrice.toString());

            logger.info("[AsianOverlandAdapter.orderCheck] 订单校验成功 - 酒店: {}, 房型: {}, 价格: {}, createKey: {}",
                    input.getHotelId(), input.getRoomId(), policyTotalPrice, createKey);

            return response;

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.orderCheck] 订单校验失败", e);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"订单校验失败: " + e.getMessage());
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
                            String country =String.valueOf(hotel.getCountryId());
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        }
                    });

            // 房间明细与房间数
            List<QTechSearchRequest.RoomDetail> details = HeyUtil.buildQTechRoomDetails(input.getOccupancy());
            req.setRoomDetails(details);
            req.setNumberOfRooms(details != null ? details.size() : 0);
            if (details.size() != input.getRoomNum()) {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"房间数与入住信息不匹配");
            }
            // 可根据需要设置静态信息、limit、availableonly 等
            req.setAvailableonly(1);
            req.setStaticData(1);

            // 2. 调用 QTECH 搜索接口
            QTechSearchResponse searchResponse = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheckOrg] 酒店搜索失败:{}", e.getMessage()))
                    .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.orderCheckOrg] 酒店搜索失败:" + e.getMessage()))
                    .block();

            // 3. 如果有搜索响应，添加到结果中
            if (searchResponse != null) {
                result.put("searchResponse", searchResponse);
                logger.info("[AsianOverlandAdapter.orderCheckOrg] 酒店搜索响应已获取: message={}", searchResponse.getMessage());

                // 4. 如果搜索响应成功并获得了searchUniqueId，继续调用取消规则接口
                if ("success".equalsIgnoreCase(searchResponse.getMessage()) && StrUtil.isNotBlank(searchResponse.getSearchUniqueId())) {

                    // 仅处理指定酒店ID的报价
                    Optional<QTechSearchResponse.Hotel> targetHotelOpt = searchResponse.getHotelList().stream()
                            .filter(h -> input.getHotelId().equals(h.getHotelId()))
                            .findFirst();
                    if (targetHotelOpt.isEmpty()) {
                        throw SupplierException.notFound(getSafeSupplierName(),"返回酒店列表中不包含请求的酒店ID: " + input.getHotelId());
                    }

                    // 使用共用的转换方法
                    QTechSearchResponse.Hotel targetHotel = targetHotelOpt.get();
                    List<XRoom> xRooms = convertHotelToXRooms(targetHotel, input.getCheckInDate(), input.getCheckOutDate(), input.getRoomNum());

                    if (xRooms.isEmpty()) {
                        throw SupplierException.notFound(getSafeSupplierName(),"无有效房型数据");
                    }
                    logger.info("[AsianOverlandAdapter.orderCheck] 单酒店报价完成，酒店:{} 返回:{}个房型,总价:{}", targetHotel.getHotelId(), xRooms.size(), targetHotel.getTotalCharges());

                    String searchUniqueId = searchResponse.getSearchUniqueId();
                    logger.info("[AsianOverlandAdapter.orderCheckOrg] 获取到searchUniqueId: {}, 继续调用取消规则接口", searchUniqueId);
                    if (StrUtil.isBlank(input.getRoomId())) {
                        throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少房型ID");
                    }
                    // 找到匹配的房型（input.getRoomId() = room.getRoomId()）
                    XRoom matchedRoom = xRooms.stream().parallel()
                            .filter(r -> input.getRoomId().equals(r.getRoomId()))
                            .findFirst()
                            .orElseThrow(() -> SupplierException.invalidParameter(getSafeSupplierName(),"未找到匹配的房型: " + input.getRoomId()));

                    //从房型扩展信息里获取 房型唯一标识（sectionUniqueId）和 房间类型ID（classUniqueId）
                    QTechSearchResponse.RoomRateExt roomRateExt = JSONUtil.toBean(matchedRoom.getExt(), QTechSearchResponse.RoomRateExt.class);
                    if (roomRateExt == null || StrUtil.isBlank(roomRateExt.getSectionUniqueId()) || CollUtil.isEmpty(roomRateExt.getClassUniqueId())) {
                        throw SupplierException.invalidParameter(getSafeSupplierName(),"所预定的房型扩展信息缺失，无法预定");
                    }

                    // 调用取消规则接口
                    QTechCancellationPolicyRequest policyRequest = new QTechCancellationPolicyRequest();
                    policyRequest.setHotelId(input.getHotelId());
                    policyRequest.setUniqueId(searchUniqueId);
                    policyRequest.setSectionUniqueId(roomRateExt.getSectionUniqueId()); // 房型唯一标识，每次查询酒店报价的唯一标识（动态变化）

                    QTechCancellationPolicyResponse policyResponse = getCancellationPolicy(policyRequest)
                            .doOnError(e -> logger.error("[AsianOverlandAdapter.orderCheckOrg] 获取取消规则失败:{}", e.getMessage()))
                            .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.orderCheckOrg] 获取取消规则失败:" + e.getMessage()))
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
                throw SupplierException.invalidParameter(getSafeSupplierName(),"缺少酒店ID");
            }


            // 币种，默认 USD
            req.setSelCurrency("USD");

            //从当前酒店详细里获取 : 目的地国家/目的地城市/国籍/居住国
            staticDataQueryService.getHotelByHotelCode(getSafeSupplierId(), getSafeSupplierName(), hotelId)
                    .ifPresent(hotel -> {
                        if (hotel != null) {
                            String country =String.valueOf(hotel.getCountryId());
                            req.setSelNationality(country);
                            req.setCountryOfResidence(country);
                        }
                    });

            // 房间明细
            req.setRoomDetails(HeyUtil.buildQTechRoomDetails("2"));

            // 调用 QTECH 搜索
            QTechSearchResponse response = this.searchHotels(req)
                    .doOnError(e -> logger.error("[AsianOverlandAdapter.getHotelRoomOrigContent] 酒店搜索失败:{}", e.getMessage()))
                    .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getHotelRoomOrigContent] 获取原始报价失败:" + e.getMessage()))
                    .timeout(Duration.ofSeconds(90), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getHotelRoomOrigContent] 获取原始报价超时")))
                    .block();


            if (response != null && response.getMessage() != null && response.getMessage().equalsIgnoreCase("success")) {
                logger.info("[AsianOverlandAdapter.getHotelRoomOrigContent] 成功获取原始报价数据");
                if (StrUtil.isBlank(response.getSearchUniqueId())) {
                    return response;
                }

                // 2. 获取酒店详情
                QTechHotelDetailRequest detailRequest = new QTechHotelDetailRequest();
                detailRequest.setHotelId(hotelId);
                detailRequest.setUniqueId(response.getSearchUniqueId());

                QTechHotelDetailResponse detailResponse = this.getHotelDetail(detailRequest)
                        .doOnError(e -> logger.error("[AsianOverlandAdapter.getHotelRoomOrigContent] 获取酒店详情失败:{}", e.getMessage()))
                        .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getHotelRoomOrigContent] 获取酒店详情失败:" + e.getMessage()))
                        .timeout(Duration.ofSeconds(60), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.getHotelRoomOrigContent] 获取酒店详情超时")))
                        .block();

                return detailResponse; // 返回原始响应对象
            } else {
                throw SupplierException.invalidParameter(getSafeSupplierName(),"获取原始数据失败: 供应商返回失败状态");
            }

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.getHotelRoomOrigContent] 获取原始报价失败", e);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"获取原始数据失败: " + e.getMessage());
        }
    }

    // ============================================ 工具方法 ==============================================

    /**
     * 执行预定流程：预定接口 + 超时处理 + 订单详情轮询
     * <p>
     * 流程说明：
     * 1. 提交预定请求，设置超时时间（180s）
     * 2. 如果预定接口超时或失败，异步调用订单详情接口
     * 3. 使用agentRefNo轮询订单详情，每隔3秒调用一次，最多10次
     * 4. 根据最终状态返回：预定中｜预定成功｜预定失败
     *
     * @param reservationRequest 预定请求
     * @param input              创建订单请求
     * @return 预定响应
     */
    private XCreateOrderResponse executeBookingWithTimeoutAndPolling(QTechReservationRequest reservationRequest, XCreateOrderRequest input) {
        String agentRefNo = reservationRequest.getAgentRefNo(); // 订单唯一号 ，
        logger.info("[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 开始执行预定流程，订单号: {}", agentRefNo);


        // 1. 提交预定请求，设置超时时间为8秒
        QTechReservationResponse reservationResponse = this.bookHotel(reservationRequest)
                .timeout(Duration.ofSeconds(180), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"预定接口调用超时")))
                .doOnError(e -> logger.error("预定接口调用失败: {}", e.getMessage()))
                .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 预定接口调用失败:" + e.getMessage()))
                .block();

        // 2. 检查预定响应
        if (reservationResponse != null && isBookingSuccessful(reservationResponse)) {
            logger.info("[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 预定接口响应成功，订单号: {}", agentRefNo);
            return buildSuccessfulOrderResponse(reservationResponse, input);
        }

        // 3. 预定接口超时或失败，开始轮询订单详情
        if (reservationResponse != null && isBookingFailed(reservationResponse)) {
            logger.warn("[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 预定接口响应失败，订单号: {},响应消息：{}, 状态：{}", agentRefNo,
                    reservationResponse != null ? reservationResponse.getMessageInfo() : "null",
                    reservationResponse != null ? reservationResponse.getMessage() : "null");

            throw SupplierException.invalidParameter(getSafeSupplierName(),"预定接口响应失败:" + reservationResponse.getMessageInfo());
        }


        QTechBookingDetailResponse finalDetailResponse = pollBookingDetails(agentRefNo);

        // 4. 根据订单详情响应构建最终结果
        if (finalDetailResponse != null && isBookingDetailFinalStatus(finalDetailResponse)) {
            String currentStatus = finalDetailResponse.getBookingDetail() != null ?
                    finalDetailResponse.getBookingDetail().getCurrentStatus() : "unknown";
            logger.info("[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 通过订单详情获取到最终状态: {}, 订单号: {}",
                    currentStatus, agentRefNo);
            return buildOrderResponseFromDetail(finalDetailResponse);
        } else {
            // 轮询未获取到最终状态，根据最后一次响应判断
            if (finalDetailResponse != null && finalDetailResponse.getBookingDetail() != null) {
                String currentStatus = finalDetailResponse.getBookingDetail().getCurrentStatus();
                logger.warn("[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 订单详情轮询未完成，当前状态: {}, 订单号: {}",
                        currentStatus, agentRefNo);

                // 如果是需要继续轮询的状态，返回处理中状态
                if (shouldContinuePolling(currentStatus)) {
                    throw SupplierException.invalidParameter(getSafeSupplierName(),"预定失败", buildPendingOrderResponse(agentRefNo, reservationRequest, input));
                } else {
                    // 如果是最终状态但之前判断有误，直接构建响应
                    throw SupplierException.invalidParameter(getSafeSupplierName(),"预定失败", buildOrderResponseFromDetail(finalDetailResponse));
                }
            } else {
                logger.warn("[AsianOverlandAdapter.executeBookingWithTimeoutAndPolling] 订单详情轮询失败，返回预定中状态，订单号: {}", agentRefNo);
                throw SupplierException.invalidParameter(getSafeSupplierName(),"预定失败", buildPendingOrderResponse(agentRefNo, reservationRequest, input));
            }
        }
    }

    /**
     * 轮询订单详情
     * 每隔3秒调用一次，最多轮询3次
     *
     * @param agentRefNo 订单唯一号
     * @return 订单详情响应
     */
    private QTechBookingDetailResponse pollBookingDetails(String agentRefNo) {
        int maxRetries = 10;
        int retryInterval = 3000; // 3秒

        for (int i = 0; i < maxRetries; i++) {
            try {
                logger.info("[AsianOverlandAdapter.pollBookingDetails] 第{}次轮询订单详情，订单号: {}", i + 1, agentRefNo);

                QTechBookingDetailRequest detailRequest = new QTechBookingDetailRequest();
                detailRequest.setBookingId(agentRefNo);

                QTechBookingDetailResponse detailResponse = this.getBookingDetail(detailRequest)
                        .timeout(Duration.ofSeconds(10), Mono.error(SupplierException.invalidParameter(getSafeSupplierName(),"订单详情接口调用超时")))
                        .doOnError(e -> logger.error("[AsianOverlandAdapter.pollBookingDetails] 订单详情接口调用失败:{}", e.getMessage()))
                        .onErrorMap(e -> SupplierException.invalidParameter(getSafeSupplierName(),"[AsianOverlandAdapter.pollBookingDetails] 订单详情接口调用失败:" + e.getMessage()))
                        .block();

                if (detailResponse != null && isBookingDetailFinalStatus(detailResponse)) {
                    String currentStatus = detailResponse.getBookingDetail() != null ?
                            detailResponse.getBookingDetail().getCurrentStatus() : "unknown";
                    logger.info("[AsianOverlandAdapter.pollBookingDetails] 订单详情获取到最终状态: {}, 订单号: {}",
                            currentStatus, agentRefNo);
                    return detailResponse;
                }

                // 如果不是最后一次重试，等待2秒后继续
                if (i < maxRetries - 1) {
                    logger.info("[AsianOverlandAdapter.pollBookingDetails] 等待{}ms后进行下次轮询", retryInterval);
                    Thread.sleep(retryInterval);
                }

            } catch (InterruptedException e) {
                logger.error("[AsianOverlandAdapter.pollBookingDetails] 轮询被中断", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("[AsianOverlandAdapter.pollBookingDetails] 轮询异常", e);
            }
        }

        logger.warn("[AsianOverlandAdapter.pollBookingDetails] 订单详情轮询完成，未获取到有效响应，订单号: {}", agentRefNo);
        return null;
    }

    /**
     * 判断预定是否成功
     */
    private boolean isBookingSuccessful(QTechReservationResponse response) {
        return response != null &&
                "success".equalsIgnoreCase(response.getMessage()) &&
                response.getBookingDetail() != null &&
                response.getBookingDetail().getId() != null &&
                (response.getBookingDetail().getCurrentStatus().equals(QTechBookingStatusEnum.VOUCHERED.getCode()) ||
                        response.getBookingDetail().getCurrentStatus().equals(QTechBookingStatusEnum.ON_REQUEST.getCode())
                );
    }

    /**
     * 判断预定是否失败
     */
    private boolean isBookingFailed(QTechReservationResponse response) {
        return response != null &&
                "fail".equalsIgnoreCase(response.getMessage()) &&
                response.getBookingDetail() == null;
    }

    /**
     * 将QTECH预订状态映射为标准订单状态
     *
     * @param qtechStatus QTECH原始状态
     * @return 标准订单状态
     */
    private SupplierOrderStatusEnum mapQTechStatusToStandardStatus(String qtechStatus) {
        QTechBookingStatusEnum qtechEnum = QTechBookingStatusEnum.fromCode(qtechStatus);

        switch (qtechEnum) {
            case VOUCHERED:
                // 预订已确认
                return SupplierOrderStatusEnum.CONFIRMED;

            case ON_REQUEST:
            case CANCELLED:
                // 预订已取消
                return SupplierOrderStatusEnum.CANCELLED;
            case INPROCESS_BOOKING:
                // 预订处理中，需要轮询
                return SupplierOrderStatusEnum.PAID;

            case REJECTED:
                return SupplierOrderStatusEnum.REJECT;
            case FAILED:
                // 预订被拒绝或失败
                return SupplierOrderStatusEnum.REJECT;

            case INPROCESS_CANCEL:
                // 取消处理中
                return SupplierOrderStatusEnum.APPLY_CANCEL;

            default:
                logger.warn("[AsianOverlandAdapter.mapQTechStatusToStandardStatus] 未知的QTECH状态: {}", qtechStatus);
                return SupplierOrderStatusEnum.UNKNOWN;
        }
    }

    /**
     * 判断QTECH状态是否需要继续轮询
     *
     * @param qtechStatus QTECH原始状态
     * @return 是否需要继续轮询
     */
    private boolean shouldContinuePolling(String qtechStatus) {
        QTechBookingStatusEnum qtechEnum = QTechBookingStatusEnum.fromCode(qtechStatus);
        return qtechEnum.shouldContinuePolling();
    }

    /**
     * 判断订单详情是否为最终状态（不需要继续轮询）
     */
    private boolean isBookingDetailFinalStatus(QTechBookingDetailResponse response) {
        if (response == null || !"success".equalsIgnoreCase(response.getMessage()) || response.getBookingDetail() == null) {
            return false;
        }
        String currentStatus = response.getBookingDetail().getCurrentStatus();
        return !shouldContinuePolling(currentStatus);
    }

    /**
     * 构建成功的订单响应（基于预定接口响应）
     */
    private XCreateOrderResponse buildSuccessfulOrderResponse(QTechReservationResponse reservationResponse, XCreateOrderRequest input) {
        XCreateOrderResponse response = new XCreateOrderResponse();
        response.setStatus(SupplierOrderStatusEnum.CONFIRMED);
        response.setStatusDesc("预定成功");
        response.setSupplierOrderId(reservationResponse.getBookingDetail() != null ?
                reservationResponse.getBookingDetail().getId() : null);
        response.setOrigStatus("SUCCESS");
        response.setOrigStatusDesc("预定成功");

        // 设置价格信息
        if (reservationResponse.getBookingDetail() != null) {
            BigDecimal totalPrice = reservationResponse.getBookingDetail().getTotalCharges();
            if (totalPrice != null) {
                response.setTotalPrice(totalPrice);
                response.setTotalBasePrice(totalPrice); // 成本价暂时设为相同
                response.setCurrency(input.getCurrency()); // QTECH默认使用USD
                response.setExt("" + JSONUtil.toJsonStr(reservationResponse) + ""); // 保存原始响应
            }
        }

        String supplierOrderId = reservationResponse.getBookingDetail() != null ?
                reservationResponse.getBookingDetail().getId() : null;
        logger.info("[AsianOverlandAdapter.buildSuccessfulOrderResponse] 构建成功响应，供应商订单号: {}, 价格: {}",
                supplierOrderId, response.getTotalPrice());
        return response;
    }

    /**
     * 构建订单响应（基于订单详情响应）
     */
    private XCreateOrderResponse buildOrderResponseFromDetail(QTechBookingDetailResponse detailResponse) {
        XCreateOrderResponse response = new XCreateOrderResponse();

        // 根据订单详情状态判断最终结果，使用完善的状态映射逻辑
        String qtechStatus = "unknown";
        QTechBookingStatusEnum qtechEnum = QTechBookingStatusEnum.UNKNOWN;

        if ("success".equalsIgnoreCase(detailResponse.getMessage()) && detailResponse.getBookingDetail() != null) {
            qtechStatus = detailResponse.getBookingDetail().getCurrentStatus();
            if (qtechStatus != null) {
                qtechEnum = QTechBookingStatusEnum.fromCode(qtechStatus);
                // 使用状态映射方法
                SupplierOrderStatusEnum mappedStatus = mapQTechStatusToStandardStatus(qtechStatus);
                response.setStatus(mappedStatus);
            } else {
                response.setStatus(SupplierOrderStatusEnum.UNKNOWN);
            }
        } else {
            response.setStatus(SupplierOrderStatusEnum.UNKNOWN);
        }

        // 设置状态描述，使用枚举的描述
        response.setStatusDesc(qtechEnum.getDesc());
        response.setOrigStatus(qtechStatus);
        response.setOrigStatusDesc(qtechEnum.getDesc()); // 使用枚举的描述作为原始状态描述

        // 设置订单号和价格信息
        if (detailResponse.getBookingDetail() != null) {
            response.setSupplierOrderId(detailResponse.getBookingDetail().getId());

            response.setExt("" + JSONUtil.toJsonStr(detailResponse) + ""); // 保存原始响应

            // 设置价格信息
            BigDecimal totalPrice = detailResponse.getBookingDetail().getTotalCharges();
            if (totalPrice != null) {
                response.setTotalPrice(totalPrice);
                response.setTotalBasePrice(totalPrice); // 成本价暂时设为相同
                response.setCurrency(detailResponse.getBookingDetail().getCurrencyCode() != null ?
                        detailResponse.getBookingDetail().getCurrencyCode() : "USD");
            }
        }

        logger.info("[AsianOverlandAdapter.buildOrderResponseFromDetail] 基于订单详情构建响应，QTECH状态: {}, 映射状态: {}",
                qtechStatus, response.getStatus());
        return response;
    }

    /**
     * 构建预定中状态的订单响应
     */
    private XCreateOrderResponse buildPendingOrderResponse(String agentRefNo, QTechReservationRequest reservationRequest, XCreateOrderRequest input) {
        XCreateOrderResponse response = new XCreateOrderResponse();
        response.setStatus(SupplierOrderStatusEnum.PAID);
        response.setStatusDesc("预定处理中");
        response.setOrigStatus("PENDING");
        response.setOrigStatusDesc("预定处理中，请稍后通过订单查询接口查询最终状态");

        // 设置价格信息
        if (reservationRequest.getExpectedPrice() != null) {
            response.setTotalPrice(reservationRequest.getExpectedPrice());
            response.setTotalBasePrice(reservationRequest.getExpectedPrice()); // 成本价暂时设为相同
            response.setCurrency(input.getCurrency());
        }

        logger.info("[AsianOverlandAdapter.buildPendingOrderResponse] 构建预定中响应，订单号: {}", agentRefNo);
        return response;
    }

    /**
     * 构建失败的订单响应
     */
    private XCreateOrderResponse buildFailedOrderResponse(String agentRefNo, String errorMessage) {
        XCreateOrderResponse response = new XCreateOrderResponse();
        response.setStatus(SupplierOrderStatusEnum.REJECT);
        response.setStatusDesc("预定失败");
        response.setOrigStatus("FAILED");
        response.setOrigStatusDesc(errorMessage);

        logger.error("[AsianOverlandAdapter.buildFailedOrderResponse] 构建失败响应，订单号: {}, 错误: {}", agentRefNo, errorMessage);
        return response;
    }



    /**
     *  验证预定价格
     * 实现价格判断和设置逻辑：
     * 1、SalePrice渠道 100 < 供应商 101 = 亏 1 截断
     * 2、SalePrice渠道 120 > 供应商 101 = 加价 19 不截断，用接口获取的最新预定价去提交预定
     * 3、SalePrice渠道 100 = 供应商 100 = 不加价不截断 用接口获取的最新预定价去提交预定
     *
     * @param salePrice    预定销售价格
     * @param supplierPrice 供应商价格
     * @return 最终预定价格
     */
    private BigDecimal verifyBookingPrice(BigDecimal salePrice, BigDecimal supplierPrice) {
        // 3. 价格对比和决策逻辑
        int comparison = salePrice.compareTo(supplierPrice);
        if (comparison < 0) {
            // 情况1: SalePrice < 供应商价格 = 亏损，截断订单
            BigDecimal loss = supplierPrice.subtract(salePrice);
            logger.warn("[AsianOverlandAdapter.determineFinalBookingPrice] 价格亏损截断 - 销售价: {}, 供应商价: {}, 亏损: {}",
                    salePrice, supplierPrice, loss);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"价格亏损，无法预订 - 销售价: " + salePrice + ", 供应商价: " + supplierPrice + ", 亏损: " + loss);

        } else if (comparison > 0) {
            // 情况2: SalePrice > 供应商价格 = 加价，不截断，用供应商最新价格预定
            BigDecimal markup = salePrice.subtract(supplierPrice);
            logger.info("[AsianOverlandAdapter.determineFinalBookingPrice] 价格加价不截断 - 销售价: {}, 供应商价: {}, 加价: {}, 使用供应商价格预定",
                    salePrice, supplierPrice, markup);
            return supplierPrice;

        } else {
            // 情况3: SalePrice = 供应商价格 = 不加价不截断，用供应商最新价格预定
            logger.info("[AsianOverlandAdapter.determineFinalBookingPrice] 价格相等不截断 - 销售价: {}, 供应商价: {}, 使用供应商价格预定",
                    salePrice, supplierPrice);
            return supplierPrice;
        }
    }

    /**
     * 构建预订房间明细JSON字符串
     * 根据input.getRoomNum()构建对应数量的房间明细，使用input.getRatePlanId()作为roomClassId
     * 结合XCreateOrderRequest中的入住人信息转换为QTech预订接口需要的格式
     *
     * @param input       创建订单请求
     * @param roomDetails 搜索时构建的房间明细
     * @return 房间明细JSON字符串
     */
    private String buildReservationRoomDetails(XCreateOrderRequest input, List<QTechSearchRequest.RoomDetail> roomDetails, List<String> classUniqueIds) {
        try {
            List<QTechReservationRequest.RoomDetail> roomDetailsList = new ArrayList<>();

            // 获取房间数量，优先使用input.getRoomNum()
            int roomCount = input.getRoomNum() != null ? input.getRoomNum() : 1;
            logger.info("[AsianOverlandAdapter.buildReservationRoomDetails] 构建房间明细，房间数量: {}", roomCount);

            // 按房间分组入住人信息，重新映射为从0开始的连续索引
            Map<Integer, List<XCreateOrderRequest.CreateOrderCustomer>> roomGroups = new HashMap<>();
            if (input.getCustomers() != null && !input.getCustomers().isEmpty()) {
                // 先按原始roomIndex分组
                Map<Integer, List<XCreateOrderRequest.CreateOrderCustomer>> originalGroups = new HashMap<>();
                for (XCreateOrderRequest.CreateOrderCustomer customer : input.getCustomers()) {
                    Integer roomIndex = customer.getRoomIndex() != null ? customer.getRoomIndex() : 1;
                    originalGroups.computeIfAbsent(roomIndex, k -> new ArrayList<>()).add(customer);
                }

                // 重新映射为从0开始的连续索引
                int newIndex = 0;
                for (Integer originalIndex : originalGroups.keySet().stream().sorted().collect(Collectors.toList())) {
                    roomGroups.put(newIndex, originalGroups.get(originalIndex));
                    logger.debug("[AsianOverlandAdapter.buildReservationRoomDetails] 房间索引映射: 原始索引{} -> 新索引{}", originalIndex, newIndex);
                    newIndex++;
                }
            }

            // 根据input.getRoomNum()构建对应数量的房间明细
            for (int i = 0; i < roomCount; i++) {
                QTechReservationRequest.RoomDetail reservationRoom = new QTechReservationRequest.RoomDetail();

                // 使用搜索房间明细作为模板（如果有多个搜索房间明细，按索引取；如果只有一个，重复使用）
                QTechSearchRequest.RoomDetail templateRoom = roomDetails.get(i < roomDetails.size() ? i : 0);

                // 使用搜索房间明细的基础信息
                reservationRoom.setNumberOfAdults(templateRoom.getNumberOfAdults());
                reservationRoom.setNumberOfChilds(templateRoom.getNumberOfChild() != null ? templateRoom.getNumberOfChild().toString() : "0");

                // 设置roomClassId
                reservationRoom.setRoomClassId(classUniqueIds.get(i)); // 取第一个作为roomClassId

                logger.debug("[AsianOverlandAdapter.buildReservationRoomDetails] 房间{}设置roomClassId: {}, RatePlanId:{}", i + 1, classUniqueIds.get(i), input.getRatePlanId());

                // 获取该房间的入住人信息（修复索引：应该是i而不是i+1）
                List<XCreateOrderRequest.CreateOrderCustomer> roomCustomers = roomGroups.get(i);

                // 从实际入住人信息中提取成人和儿童
                List<QTechReservationRequest.Passenger> actualAdults = new ArrayList<>();
                List<QTechReservationRequest.Passenger> actualChildren = new ArrayList<>();

                if (roomCustomers != null && !roomCustomers.isEmpty()) {
                    for (XCreateOrderRequest.CreateOrderCustomer customer : roomCustomers) {
                        QTechReservationRequest.Passenger passenger = new QTechReservationRequest.Passenger();

                        // 判断是否为儿童（0 ～ 12岁儿童）
                        boolean isChild = customer.getAge() != null && customer.getAge() <= 12;

                        passenger.setSalutation(isChild ? "Child" : "MR");
                        passenger.setFirst_name(customer.getName() != null ? customer.getName() : "Guest");
                        passenger.setLast_name(customer.getFamilyName() != null ? customer.getFamilyName() : "");

                        if (isChild && customer.getAge() != null) {
                            passenger.setAge(customer.getAge().toString());
                        }

                        // 分类存储
                        if (isChild) {
                            actualChildren.add(passenger);
                        } else {
                            actualAdults.add(passenger);
                        }
                    }
                }

                // 补齐成人数量（修复死循环：应该是<而不是<=）
                while (actualAdults.size() < templateRoom.getNumberOfAdults()) {
                    QTechReservationRequest.Passenger passenger = new QTechReservationRequest.Passenger();

                    if (!actualAdults.isEmpty()) {
                        // 复制已有成人信息
                        QTechReservationRequest.Passenger template = actualAdults.get(0);
                        passenger.setSalutation(template.getSalutation());
                        passenger.setFirst_name(template.getFirst_name());
                        passenger.setLast_name(template.getLast_name());
                        if (template.getAge() != null) {
                            passenger.setAge(template.getAge());
                        }
                    } else {
                        // 没有已有成人信息时使用默认值
                        passenger.setSalutation("MR");
                        String randomSuffix = String.format("%03d", (int) (Math.random() * 1000));
                        passenger.setFirst_name("Guest" + randomSuffix);
                        passenger.setLast_name("Guest");
                    }
                    actualAdults.add(passenger);
                }

                // 补齐儿童数量（修复死循环：应该是<而不是<=）
                int requiredChildCount = templateRoom.getNumberOfChild() != null ? templateRoom.getNumberOfChild() : 0;
                while (actualChildren.size() < requiredChildCount) {
                    QTechReservationRequest.Passenger passenger = new QTechReservationRequest.Passenger();

                    if (!actualChildren.isEmpty()) {
                        // 复制已有儿童信息
                        QTechReservationRequest.Passenger template = actualChildren.get(0);
                        passenger.setSalutation(template.getSalutation());
                        passenger.setFirst_name(template.getFirst_name());
                        passenger.setLast_name(template.getLast_name());
                        if (template.getAge() != null) {
                            passenger.setAge(template.getAge());
                        }
                    } else {
                        // 没有已有儿童信息时使用默认值
                        passenger.setSalutation("Child");
                        String randomSuffix = String.format("%03d", (int) (Math.random() * 1000));
                        passenger.setFirst_name("Child" + randomSuffix);
                        passenger.setLast_name("Child");
                        passenger.setAge("6"); // 默认儿童年龄
                    }
                    actualChildren.add(passenger);
                }

                // 如果实际人数超过要求，截取到要求的数量
                if (actualAdults.size() > templateRoom.getNumberOfAdults()) {
                    actualAdults = actualAdults.subList(0, templateRoom.getNumberOfAdults());
                }
                if (actualChildren.size() > requiredChildCount) {
                    actualChildren = actualChildren.subList(0, requiredChildCount);
                }

                // 合并成人和儿童列表（成人在前，儿童在后）
                List<QTechReservationRequest.Passenger> passengers = new ArrayList<>();
                passengers.addAll(actualAdults);
                passengers.addAll(actualChildren);

                reservationRoom.setPassangers(passengers);
                roomDetailsList.add(reservationRoom);
            }

            logger.info("[AsianOverlandAdapter.buildReservationRoomDetails] 成功构建{}间房的预订明细", roomDetailsList.size());

            // 转换为JSON字符串
            return JSONUtil.toJsonStr(roomDetailsList);

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.buildReservationRoomDetails] 构建房间明细失败", e);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"构建预定房间明细失败");
        }
    }



    /**
     * 验证价格一致性
     * 对比搜索结果价格与取消规则接口价格，必须完全相等
     *
     * @param searchPrice  搜索结果价格（BigDecimal格式）
     * @param bookingPrice 预定价格（BigDecimal格式）
     * @return true-价格一致，false-价格不一致
     */
    private boolean validatePriceConsistency(BigDecimal searchPrice, BigDecimal bookingPrice) {
        try {
            if (searchPrice == null || bookingPrice == null) {
                logger.warn("[AsianOverlandAdapter.validatePriceConsistency] 价格参数为空: searchPrice={}, policyPrice={}",
                        searchPrice, bookingPrice);
                return false;
            }

            // 使用BigDecimal的compareTo方法进行精确比较（必须完全相等）
            boolean isEqual = searchPrice.compareTo(bookingPrice) == 0;

            logger.debug("[AsianOverlandAdapter.validatePriceConsistency] 价格对比结果: 搜索价格={}, 取消规则价格={}, 是否相等={}",
                    searchPrice, bookingPrice, isEqual);

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
     * @param hotel        QTech酒店响应数据
     * @param checkInDate  入住日期
     * @param checkOutDate 退房日期
     * @param roomNum      房间数量
     * @return 转换后的房型列表
     */
    private List<XRoom> convertHotelToXRooms(QTechSearchResponse.Hotel hotel, LocalDateTime checkInDate, LocalDateTime checkOutDate, Integer roomNum) {


        try {
            //房型列表
            List<XRoom> xRooms = new ArrayList<>();
            List<QTechSearchResponse.HotelProperty> properties = hotel.getHotelProperty();
            logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 处理酒店: ID={}, 名称={},总价={}",
                    hotel.getHotelId(), hotel.getHotelName(), hotel.getTotalCharges());

            if (properties == null || properties.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无房型属性数据", hotel.getHotelId());
                return xRooms;
            }

            // 只处理Type=Selection的房型属性
            List<QTechSearchResponse.HotelProperty> targetHotelPropList = properties.stream()
                    .filter(p -> p.getType().equalsIgnoreCase("Selection")).collect(Collectors.toList());

            if (targetHotelPropList.isEmpty()) {
                logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无Selection类型属性", hotel.getHotelId());
                throw SupplierException.invalidParameter(getSafeSupplierName(),"convertHotelToXRooms 转换酒店数据失败");
            }

            // 遍历每个房型属性
            for (QTechSearchResponse.HotelProperty prop : targetHotelPropList) {

                //状态业务所需的扩展字段
                // - 房型标识  key
                // - 房型属性ID sectionUniqueId
                // - 房型报价ID classUniqueId
                QTechSearchResponse.RoomRateExt roomRateExt = new QTechSearchResponse.RoomRateExt();
                //设置所选房型属性ID
                roomRateExt.setSectionUniqueId(prop.getSectionUniqueId());


                logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 酒店属性: 星级={}, 地址={}",
                        prop.getDisplayRoomRate(), prop.getType());

                List<QTechSearchResponse.RoomRate> roomRates = prop.getRoomRates();
                if (roomRates == null || roomRates.isEmpty()) {
                    logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}无房型报价数据", hotel.getHotelId());
                    return xRooms;
                }

                if (StrUtil.isBlank(prop.getSectionUniqueId())) {
                    logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}房型属性缺少SectionUniqueId", hotel.getHotelId());
                    return xRooms;
                }


                XRoom xRoom = new XRoom();
                //classUniqueId的房型
                List<String> classUniqueIds = new ArrayList<>();

                for (QTechSearchResponse.RoomRate roomRate : roomRates) {
                    logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 处理房型: ID={}, 类型={}, 餐型={}, 价格={}, 状态={}",
                            roomRate.getClassUniqueId(), roomRate.getRoomType(), roomRate.getMealBasis(),
                            roomRate.getRoomRate(), roomRate.getAvailable());

                    if (StrUtil.isBlank(roomRate.getClassUniqueId())) {
                        logger.warn("[AsianOverlandAdapter.convertHotelToXRooms] 酒店{}房型报价缺少ClassUniqueId", hotel.getHotelId());
                        continue;
                    }

                    String roomName = roomRate.getRoomCategory();
                    if (StrUtil.isBlank(roomName)) {
                        roomName = roomRate.getRoomType();
                    }

                    // 生成房型编码： 房型名称，处理特殊字符用下划线连接
                    String roomCodeRaw = roomName;
                    // 处理特殊字符：保留字母、数字、中文，其他字符替换为下划线，连续的下划线合并为一个
                    String roomCode = roomCodeRaw.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "_")
                            .replaceAll("_+", "_")  // 合并连续的下划线
                            .replaceAll("^_|_$", ""); // 去掉首尾的下划线

                    // 如果处理后的编码超过64字符，使用MD5
                    String finalRoomCode = roomCode.length() > 64 ? MD5Util.string2MD5(roomCode) : roomCode;

                    // 设置房型标识
                    roomRateExt.setKey(finalRoomCode);
                    // 设置所选房型报价ID
                    classUniqueIds.add(roomRate.getClassUniqueId());

                    xRoom.setRoomId(finalRoomCode);
                    xRoom.setRoomName(roomRate.getRoomCategory());
                    xRoom.setRoomNameEn(roomRate.getRoomCategory());
                    xRoom.setBedTypeDescEn(roomRate.getRoomType());

                    // 禁烟
                    if (roomRate.getRoomType().contains("Non Smoking") || roomRate.getRoomType().contains("Smoking") ||
                            roomRate.getRoomCategory().contains("Non Smoking") || roomRate.getRoomCategory().contains("Smoking")) {
                        xRoom.setNoSmoking(XEnumNoSmoking.NON_SMOKING);
                    }

                    List<XRatePlan> ratePlans = new ArrayList<>();
                    XRatePlan ratePlan = new XRatePlan();

                    // 是否可退
                    String isRefundable = prop.getRefundable() ? "1" : "0";

                    // 生成房型价格计划编码： 房型编码+餐型+是否可退，处理特殊字符用下划线连接
                    String ratePlanCodeRaw = finalRoomCode + " " + roomRate.getMealCode() + " " + isRefundable;
                    // 处理特殊字符：保留字母、数字、中文，其他字符替换为下划线，连续的下划线合并为一个
                    String ratePlanCode = ratePlanCodeRaw.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "_")
                            .replaceAll("_+", "_")  // 合并连续的下划线
                            .replaceAll("^_|_$", ""); // 去掉首尾的下划线

                    // 如果处理后的编码超过64字符，使用MD5
                    String finalRatePlanCode = ratePlanCode.length() > 64 ? MD5Util.string2MD5(ratePlanCode) : ratePlanCode;

                    ratePlan.setRatePlanId(finalRatePlanCode);
                    ratePlan.setRatePlanName(roomRate.getRoomType());
                    ratePlan.setDescription(roomRate.getRoomType());
                    ratePlan.setAvailable(roomRate.getAvailable());

                    //货币种类
                    ratePlan.setCurrency(HeyUtil.toXwCurrency(hotel.getRateCurrencyCode()).orElse(XEnumCurrency.USD));
                    // 预付方式
                    ratePlan.setPayType(XEnumPayType.PREPAID);
                    //餐食类型 未知
                    ratePlan.setMealType(XMealType.UNKNOWN);
                    // 是否带餐食
                    if (roomRate.getMealBasis().contains("breakfast") || roomRate.getRoomType().contains("breakfast")) {
                        ratePlan.setBreakfast(1);
                        ratePlan.setMealType(XMealType.SPECIFY);
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

                    // 设置取消规则
                    if (ratePlan.getCancelable()) {
                        //取消规则
                        List<XRatePlan.XCancelRule> xCancelRules = new ArrayList<>();

                        // 找到最早的取消政策开始时间，用于生成免费取消规则
                        String earliestPolicyStartTime = null;
                        if (!cancellationPolicy.isEmpty()) {
                            earliestPolicyStartTime = cancellationPolicy.stream()
                                    .map(QTechSearchResponse.CancellationPolicy::getStart)
                                    .min(String::compareTo)
                                    .orElse(null);
                        }

                        // 遍历所有取消政策，生成收费取消规则
                        cancellationPolicy.forEach(policy -> {
                            XRatePlan.XCancelRule xCancelRule = new XRatePlan.XCancelRule();

                            xCancelRule.setStartTimeOrig(policy.getStart());
                            xCancelRule.setEndTimeOrig(policy.getEnd());

                            ZonedDateTime startTime = HeyUtil.convertTimeZone(policy.getStart());
                            ZonedDateTime endTime = HeyUtil.convertTimeZone(policy.getEnd());

                            xCancelRule.setStartTime(HeyUtil.formatZonedDateTimeZone(startTime));
                            xCancelRule.setEndTime(HeyUtil.formatZonedDateTimeZone(endTime));

                            xCancelRule.setDeductValue(String.valueOf(policy.getCharges()));
                            xCancelRule.setDeductType(XEnumDeductType.MONEY);
                            xCancelRules.add(xCancelRule);
                        });

                        // 生成免费取消规则：在最早取消政策开始时间之前取消是免费的
                        if (earliestPolicyStartTime != null) {
                            XRatePlan.XCancelRule freeCancelRule = new XRatePlan.XCancelRule();

                            // 当前时间（上海时区）
                            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
                            String nowFormatted = HeyUtil.formatZonedDateTimeZone(now);

                            // 最早取消政策的开始时间（转换为上海时区）
                            ZonedDateTime earliestStartTime = HeyUtil.convertTimeZone(earliestPolicyStartTime);
                            String earliestStartFormatted = HeyUtil.formatZonedDateTimeZone(earliestStartTime);

                            // 设置免费取消规则
                            freeCancelRule.setStartTimeOrig(nowFormatted);
                            freeCancelRule.setEndTimeOrig(earliestStartFormatted);
                            freeCancelRule.setStartTime(nowFormatted);
                            freeCancelRule.setEndTime(earliestStartFormatted);
                            freeCancelRule.setDeductValue("0");
                            freeCancelRule.setDeductType(XEnumDeductType.FREE);

                            // 将免费取消规则插入到列表开头（最优先）
                            xCancelRules.add(0, freeCancelRule);
                        }

                        //设置取消规则
                        ratePlan.setCancelRules(xCancelRules);
                    }

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
                        daily.setMealType(XMealType.UNKNOWN);
                        dailyPrices.add(daily);
                    });
                    ratePlan.setDailys(dailyPrices);

                    //设置 入住信息
                    ratePlan.setCheckInDate(checkInDate);
                    ratePlan.setCheckOutDate(checkOutDate);
                    ratePlan.setQuantity(roomNum);

                    //添加到房价列表
                    ratePlans.add(ratePlan);

                    //设置 房型价格
                    xRoom.setRatePlans(ratePlans);
                    //设置 房型报价
                    xRoom.setMinPrice(prop.getDisplayRoomRate());
                    xRoom.setMinBasePrice(prop.getDisplayRoomRate());


                    logger.debug("[AsianOverlandAdapter.convertHotelToXRooms] 转换报价: {},ID={},CODE={},NMAME={}", xRoom.getMinPrice(), xRoom.getRoomId(), xRoom.getRoomId(), xRoom.getRoomName());
                }

                roomRateExt.setClassUniqueId(classUniqueIds);
                //设置扩展字段
                xRoom.setExt(JSONUtil.toJsonStr(roomRateExt));

                //添加到房型列表
                xRooms.add(xRoom);
            }

            return xRooms;

        } catch (Exception e) {
            logger.error("[AsianOverlandAdapter.convertHotelToXRooms] 转换酒店{}数据失败", hotel.getHotelId(), e);
            throw SupplierException.invalidParameter(getSafeSupplierName(),"convertHotelToXRooms 转换酒店数据失败");
        }

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

    /**
     * 构建取消订单成功响应
     */
    private XCancelOrderResponse buildCancelSuccessResponse(String supplierOrderId,
                                                            QTechCancellationChargesResponse chargesResponse,
                                                            QTechCancellationResponse cancellationResponse) {
        XCancelOrderResponse response = new XCancelOrderResponse();
        // 使用现有的枚举值，取消成功可以用CANCELLED表示
        response.setStatus(SupplierOrderStatusEnum.CANCELLED);
        response.setStatusDesc("取消成功");
        response.setRefundFee(chargesResponse.getCancellationCharge());
        response.setOrigStatusDesc(cancellationResponse.getMessageInfo());

        // 记录取消费用响应的信息
        logger.info("[AsianOverlandAdapter.buildCancelSuccessResponse] 取消费用: {} {}",
                chargesResponse.getCancellationCharge(), chargesResponse.getDisplayCurrencyCode());
        response.setOrigStatus(cancellationResponse.getMessage());

        logger.info("[AsianOverlandAdapter.buildCancelSuccessResponse] 构建取消成功响应，订单号: {}", supplierOrderId);
        return response;
    }

    /**
     * 构建取消订单不允许响应
     */
    private XCancelOrderResponse buildCancelNotAllowedResponse(String supplierOrderId, String message) {
        XCancelOrderResponse response = new XCancelOrderResponse();
        // 使用REJECT表示不允许取消
        response.setStatus(SupplierOrderStatusEnum.REJECT);
        response.setStatusDesc("不允许取消");
        response.setOrigStatus("NOT_ALLOWED");
        response.setOrigStatusDesc(message != null ? message : "订单不允许取消");

        logger.warn("[AsianOverlandAdapter.buildCancelNotAllowedResponse] 构建不允许取消响应，订单号: {}, 原因: {}",
                supplierOrderId, message);
        return response;
    }

    /**
     * 构建取消订单失败响应
     */
    private XCancelOrderResponse buildCancelFailedResponse(String supplierOrderId, String errorMessage) {
        XCancelOrderResponse response = new XCancelOrderResponse();
        // 使用REJECT表示取消失败
        response.setStatus(SupplierOrderStatusEnum.CANCEL_REFUSED);
        response.setStatusDesc("取消失败");
        response.setOrigStatus("FAILED");
        response.setOrigStatusDesc(errorMessage);

        logger.error("[AsianOverlandAdapter.buildCancelFailedResponse] 构建取消失败响应，订单号: {}, 错误: {}",
                supplierOrderId, errorMessage);
        return response;
    }

    /**
     * 构建查询预定订单成功响应
     */
    private XQueryOrderResponse buildQuerySuccessResponse(String distributorOrderId, String supplierOrderId,
                                                          QTechBookingDetailResponse detailResponse) {
        XQueryOrderResponse response = new XQueryOrderResponse();

        // 设置基本信息
        response.setDistributorOrderId(distributorOrderId);
        response.setSupplierOrderId(supplierOrderId);

        // 获取并映射订单状态
        String qtechStatus = "unknown";
        QTechBookingStatusEnum qtechEnum = QTechBookingStatusEnum.UNKNOWN;

        if (detailResponse.getBookingDetail() != null) {
            qtechStatus = detailResponse.getBookingDetail().getCurrentStatus();
            if (qtechStatus != null) {
                qtechEnum = QTechBookingStatusEnum.fromCode(qtechStatus);
                // 使用状态映射方法
                SupplierOrderStatusEnum mappedStatus = mapQTechStatusToStandardStatus(qtechStatus);
                response.setStatus(mappedStatus);
            } else {
                response.setStatus(SupplierOrderStatusEnum.UNKNOWN);
            }
        } else {
            response.setStatus(SupplierOrderStatusEnum.UNKNOWN);
        }

        // 设置状态描述，使用枚举的描述
        response.setStatusDesc(qtechEnum.getDesc());
        response.setOrigStatus(qtechStatus);
        response.setOrigStatusDesc(qtechEnum.getDesc());

        // 设置订单详细信息
        if (detailResponse.getBookingDetail() != null) {
            QTechBookingDetailResponse.BookingDetail detail = detailResponse.getBookingDetail();

            // 设置价格信息
            if (detail.getTotalCharges() != null) {
                response.setTotalPrice(detail.getTotalCharges());
                response.setCurrency(detail.getCurrencyCode() != null ? detail.getCurrencyCode() : "USD");
            }

            // 设置  预定ID  和 分销商系统订单号  预定确认号
            response.setSupplierOrderId(detail.getId());
            response.setDistributorOrderId(detail.getAgentRefNo());
            response.setConfirmNo(detail.getBookingReference());


            response.setCheckInDate(HeyUtil.parseToLocalDateTime(detail.getCheckInDate()));
            response.setCheckOutDate(HeyUtil.parseToLocalDateTime(detail.getCheckOutDate()));
            response.setBookDate(HeyUtil.parseToLocalDateTime(detail.getBookingDate()));
            response.setLatestArrivalTime(HeyUtil.parseToLocalDateTime(detail.getExpirationDate()));

            response.setRoomNum(Integer.parseInt(detail.getTotalRooms() != null ? detail.getTotalRooms() : "1"));
            response.setDesc(detail.getSpecialRemark());
            response.setContactName(detail.getLeaderFirstName() + " " + detail.getLeaderLastName());


            response.setHotelName(detail.getHotelName());
            response.setHotelId(detail.getLocalHotelId());
            response.setHotelPhone(detail.getHotelPhone());
            response.setHotelAddress(detail.getCountryName() + detail.getCityId() + detail.getHotelAddress1());


            // 记录详细信息到日志（因为XQueryOrderResponse可能没有这些字段）
            logger.info("[AsianOverlandAdapter.buildQuerySuccessResponse] 订单详情 - 状态: {}, 价格: {} {}, 预订号: {}, 凭证ID: {}, 预订日期: {}",
                    qtechStatus, detail.getTotalCharges(), detail.getCurrencyCode(),
                    detail.getBookingReference(), detail.getVoucherId(), detail.getBookingDate());
        }

        logger.info("[AsianOverlandAdapter.buildQuerySuccessResponse] 构建查询成功响应，订单号: {}, 状态: {}",
                supplierOrderId, response.getStatus());
        return response;
    }

    /**
     * 构建查询订单失败响应
     */
    private XQueryOrderResponse buildQueryFailedResponse(String distributorOrderId, String supplierOrderId, String errorMessage) {
        XQueryOrderResponse response = new XQueryOrderResponse();

        // 设置基本信息
        response.setDistributorOrderId(distributorOrderId);
        response.setSupplierOrderId(supplierOrderId);

        // 设置失败状态
        response.setStatus(SupplierOrderStatusEnum.UNKNOWN);
        response.setStatusDesc("查询失败");
        response.setOrigStatus("FAILED");
        response.setOrigStatusDesc(errorMessage);

        logger.error("[AsianOverlandAdapter.buildQueryFailedResponse] 构建查询失败响应，订单号: {}, 错误: {}",
                supplierOrderId, errorMessage);
        return response;
    }

}
