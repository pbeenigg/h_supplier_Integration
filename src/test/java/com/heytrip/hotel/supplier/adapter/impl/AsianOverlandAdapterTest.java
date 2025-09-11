package com.heytrip.hotel.supplier.adapter.impl;

import com.heytrip.hotel.supplier.client.HttpClientService;
import com.heytrip.hotel.supplier.dto.qtech.req.*;
import com.heytrip.hotel.supplier.dto.qtech.resp.*;
import com.heytrip.hotel.supplier.entity.SupplierConfig;
import com.heytrip.hotel.supplier.repository.SupplierConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * AsianOverlandAdapter单元测试类
 * 测试重构后的所有API接口方法
 */
@ExtendWith(MockitoExtension.class)
class AsianOverlandAdapterTest {

    @Mock
    private HttpClientService httpClientService;

    @Mock
    private SupplierConfigRepository supplierConfigRepository;

    @InjectMocks
    private AsianOverlandAdapter asianOverlandAdapter;

    private SupplierConfig mockSupplierConfig;

    @BeforeEach
    void setUp() throws Exception {
        // 设置模拟的供应商配置
        mockSupplierConfig = new SupplierConfig();
        mockSupplierConfig.setId(1L);
        mockSupplierConfig.setSupplierName("AsianOverland");
        mockSupplierConfig.setAuthConfig("{\"username\":\"test_user\",\"password\":\"test_pass\"}");
        
        lenient().when(supplierConfigRepository.findBySupplierName("AsianOverland"))
            .thenReturn(Optional.of(mockSupplierConfig));
            
        // 通过反射设置supplierConfig字段，这样extractFromAuthConfig方法就能正常工作
        java.lang.reflect.Field supplierConfigField = AsianOverlandAdapter.class.getSuperclass()
            .getDeclaredField("supplierConfig");
        supplierConfigField.setAccessible(true);
        supplierConfigField.set(asianOverlandAdapter, mockSupplierConfig);
        
        // 设置通用的HTTP客户端模拟响应，使用lenient模式避免参数不匹配
        setupHttpClientMocks();
    }

    @Test
    void testSearchHotels_success() {
        // 准备测试数据
        QTechSearchRequest request = new QTechSearchRequest();
        request.setUsername("test_user");
        request.setPassword("test_pass");
        request.setSelCity("Bangkok");
        request.setCheckinDate("15/06/2024");
        request.setCheckoutDate("17/06/2024");
        request.setNumberOfRooms(1);
        request.setRoomDetails("[{\"numberOfAdults\":2,\"numberOfChild\":0}]");

        // HTTP客户端响应已在setupHttpClientMocks中设置

        // 执行测试
        Mono<QTechSearchResponse> result = asianOverlandAdapter.searchHotels(request);

        // 验证结果
        QTechSearchResponse actualResponse = result.block();
        assertNotNull(actualResponse);

        // 验证HTTP客户端被正确调用
        verify(httpClientService).get(
            anyString(),
            contains("/ws/index.php?"),
            eq(QTechSearchResponse.class),
            any(),
            anyLong()
        );
    }

    @Test
    void testGetHotelDetail_success() {
        // 准备测试数据
        QTechHotelDetailRequest request = new QTechHotelDetailRequest();
        request.setUsername("test_user");
        request.setPassword("test_pass");
        request.setHotelId("12345");

        QTechHotelDetailResponse mockResponse = new QTechHotelDetailResponse();

        // 模拟HTTP客户端响应
        when(httpClientService.get(anyString(), anyString(), eq(QTechHotelDetailResponse.class), 
                                 any(), anyLong()))
            .thenReturn(Mono.just(mockResponse));

        // 执行测试
        Mono<QTechHotelDetailResponse> result = asianOverlandAdapter.getHotelDetail(request);

        // 验证结果
        QTechHotelDetailResponse actualResponse = result.block();
        assertNotNull(actualResponse);

        // 验证HTTP客户端被正确调用
        verify(httpClientService).get(
            anyString(),
            contains("hotel_id=12345"),
            eq(QTechHotelDetailResponse.class),
            any(),
            anyLong()
        );
    }

    @Test
    void testBookHotel_success() {
        // 准备测试数据
        QTechReservationRequest request = new QTechReservationRequest();
        request.setUsername("test_user");
        request.setPassword("test_pass");
        request.setHotelId("12345");
        request.setSectionUniqueId("67890");
        request.setAgentRefNo("REF123");

        QTechReservationResponse mockResponse = new QTechReservationResponse();
        mockResponse.setStatus("success");

        QTechCancellationPolicyResponse mockPolicyResponse = new QTechCancellationPolicyResponse();
        mockPolicyResponse.setTotalBookingAmount(new BigDecimal("150.00"));

        // 模拟HTTP客户端响应 - 先返回取消规则，再返回预订结果
        when(httpClientService.get(anyString(), anyString(), eq(QTechCancellationPolicyResponse.class), 
                                 any(), anyLong()))
            .thenReturn(Mono.just(mockPolicyResponse));
        
        when(httpClientService.get(anyString(), anyString(), eq(QTechReservationResponse.class), 
                                 any(), anyLong()))
            .thenReturn(Mono.just(mockResponse));

        // 执行测试
        Mono<QTechReservationResponse> result = asianOverlandAdapter.bookHotel(request);

        // 验证结果
        QTechReservationResponse actualResponse = result.block();
        assertNotNull(actualResponse);
        assertEquals("success", actualResponse.getStatus());

        // 验证HTTP客户端被调用了两次（取消规则 + 预订）
        verify(httpClientService, times(2)).get(
            anyString(),
            anyString(),
            any(),
            any(),
            anyLong()
        );
    }

    @Test
    void testGetBookingDetail_success() {
        // 准备测试数据
        QTechBookingDetailRequest request = new QTechBookingDetailRequest();
        request.setUsername("test_user");
        request.setPassword("test_pass");
        request.setBookingId("BK123456");

        QTechBookingDetailResponse mockResponse = new QTechBookingDetailResponse();

        // 模拟HTTP客户端响应
        when(httpClientService.get(anyString(), anyString(), eq(QTechBookingDetailResponse.class), 
                                 any(), anyLong()))
            .thenReturn(Mono.just(mockResponse));

        // 执行测试
        Mono<QTechBookingDetailResponse> result = asianOverlandAdapter.getBookingDetail(request);

        // 验证结果
        QTechBookingDetailResponse actualResponse = result.block();
        assertNotNull(actualResponse);

        // 验证HTTP客户端被正确调用
        verify(httpClientService).get(
            anyString(),
            contains("booking_id=BK123456"),
            eq(QTechBookingDetailResponse.class),
            any(),
            anyLong()
        );
    }

    @Test
    void testCancelBooking_success() {
        // 准备测试数据
        QTechCancellationBookingRequest request = new QTechCancellationBookingRequest();
        request.setUsername("test_user");
        request.setPassword("test_pass");
        request.setBookingId("BK123456");
        // request.setReason("客户要求取消"); // 注释掉不存在的方法

        QTechCancellationResponse mockResponse = new QTechCancellationResponse();

        // HTTP客户端响应已在setupHttpClientMocks中设置

        // 执行测试
        Mono<QTechCancellationResponse> result = asianOverlandAdapter.cancelBooking(request);

        // 验证结果
        QTechCancellationResponse actualResponse = result.block();
        assertNotNull(actualResponse);

        // 验证HTTP客户端被正确调用
        verify(httpClientService).get(
            anyString(),
            anyString(),
            eq(QTechCancellationResponse.class),
            any(),
            anyLong()
        );
    }

    @Test
    void testSupportsCountry_success_true() {
        // 测试支持的国家（使用完整国家名称）
        assertTrue(asianOverlandAdapter.supportsCountry("Thailand")); // 泰国
        assertTrue(asianOverlandAdapter.supportsCountry("Malaysia")); // 马来西亚
        assertTrue(asianOverlandAdapter.supportsCountry("Singapore")); // 新加坡
        assertTrue(asianOverlandAdapter.supportsCountry("UAE")); // 阿联酋
        
        // 测试大小写不敏感
        assertTrue(asianOverlandAdapter.supportsCountry("thailand"));
        assertTrue(asianOverlandAdapter.supportsCountry("MALAYSIA"));
    }

    @Test
    void testSupportsCountry_success_false() {
        // 测试不支持的国家
        assertFalse(asianOverlandAdapter.supportsCountry("United States")); // 美国
        assertFalse(asianOverlandAdapter.supportsCountry("China")); // 中国
        assertFalse(asianOverlandAdapter.supportsCountry("Japan")); // 日本
        assertFalse(asianOverlandAdapter.supportsCountry("TH")); // 国家代码不支持
        assertFalse(asianOverlandAdapter.supportsCountry("MY")); // 国家代码不支持
    }

    @Test
    void testHttpClientServiceError() {
        // 准备测试数据
        QTechSearchRequest request = new QTechSearchRequest();
        request.setSelCity("Bangkok");
        request.setCheckinDate("15/06/2024");
        request.setCheckoutDate("17/06/2024");
        request.setNumberOfRooms(1);
        request.setRoomDetails("[{\"numberOfAdults\":2,\"numberOfChild\":0}]");

        // 模拟HTTP客户端抛出异常
        when(httpClientService.get(anyString(), anyString(), eq(QTechSearchResponse.class), 
                                 any(), anyLong()))
            .thenReturn(Mono.error(new RuntimeException("网络连接失败")));

        // 执行测试
        Mono<QTechSearchResponse> result = asianOverlandAdapter.searchHotels(request);

        // 验证异常处理
        assertThrows(RuntimeException.class, () -> result.block());
    }

    // 注释掉不存在的方法测试
    // @Test
    // void testGetSupplierIdentifier_返回正确的供应商标识() {
    //     String identifier = asianOverlandAdapter.getSupplierIdentifier();
    //     assertEquals("AsianOverland", identifier);
    // }

    @Test
    void testBuildQTechEndpoint_success() {
        // 准备测试数据
        QTechSearchRequest request = new QTechSearchRequest();
        request.setUsername("test_user");
        request.setPassword("test_pass");
        request.setSelCity("Bangkok");

        // 通过反射调用私有方法进行测试
        try {
            java.lang.reflect.Method method = AsianOverlandAdapter.class
                .getDeclaredMethod("buildQTechEndpoint", Object.class);
            method.setAccessible(true);
            
            String endpoint = (String) method.invoke(asianOverlandAdapter, request);
            
            // 验证端点字符串包含预期的参数
            assertNotNull(endpoint);
            assertTrue(endpoint.contains("/ws/index.php?"));
            assertTrue(endpoint.contains("username=test_user"));
            assertTrue(endpoint.contains("password=test_pass"));
            assertTrue(endpoint.contains("sel_city=Bangkok") || endpoint.contains("Bangkok"));
        } catch (Exception e) {
            fail("测试buildQTechEndpoint方法失败: " + e.getMessage());
        }
    }

    private void setupHttpClientMocks() {
        // 为搜索请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechSearchResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(new QTechSearchResponse()));
            
        // 为酒店详情请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechHotelDetailResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(new QTechHotelDetailResponse()));
            
        // 为预订详情请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechBookingDetailResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(new QTechBookingDetailResponse()));
            
        // 为取消规则请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechCancellationPolicyResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(createMockCancellationPolicyResponse()));
            
        // 为预订请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechReservationResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(new QTechReservationResponse()));
            
        // 为取消费用请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechCancellationChargesResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(new QTechCancellationChargesResponse()));
            
        // 为取消预订请求设置模拟响应
        lenient().when(httpClientService.get(anyString(), anyString(), eq(QTechCancellationResponse.class), any(), anyLong()))
            .thenReturn(Mono.just(new QTechCancellationResponse()));
    }
    
    private QTechCancellationPolicyResponse createMockCancellationPolicyResponse() {
        QTechCancellationPolicyResponse response = new QTechCancellationPolicyResponse();
        response.setTotalBookingAmount(new BigDecimal("100.00"));
        return response;
    }
}
