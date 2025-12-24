package com.heytrip.hotel.supplier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heytrip.hotel.supplier.entity.primary.SupplierConfig;
import com.heytrip.hotel.supplier.repository.primary.SupplierConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SuppliersController 单元测试类
 * 测试基于MD5签名认证的供应商接口
 *
 * @author Pax
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SuppliersControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private SupplierConfigRepository supplierConfigRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // 测试用的认证配置
    private static final String TEST_APP_ID = "heytrip_supplier_integration_pax";
    private static final String TEST_SECRET_KEY = "HeyTrip@Pax#SupplierIntegration!2025";

    // 认证头部常量
    private static final String HEADER_APP_ID = "app";
    private static final String HEADER_TIMESTAMP = "timestamp";
    private static final String HEADER_SIGNATURE = "sign";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试获取所有启用的供应商列表 - 成功场景
     */
    @Test
    void testGetEnabledSuppliers_Success() throws Exception {
        // 准备测试数据
        List<SupplierConfig> mockSuppliers = createMockSuppliers();
        when(supplierConfigRepository.findByIsActiveTrue()).thenReturn(mockSuppliers);

        // 生成认证头部
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = generateMD5Signature(TEST_APP_ID, timestamp, TEST_SECRET_KEY);

        // 执行请求并验证结果
        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, TEST_APP_ID)
                        .header(HEADER_TIMESTAMP, timestamp)
                        .header(HEADER_SIGNATURE, signature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.suppliers").isArray())
                .andExpect(jsonPath("$.suppliers.length()").value(2))
                .andExpect(jsonPath("$.suppliers[0].supplierName").value("TestSupplier1"))
                .andExpect(jsonPath("$.suppliers[1].supplierName").value("TestSupplier2"))
                .andExpect(jsonPath("$.totalSuppliers").value(2))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试获取所有启用的供应商列表 - 缺少认证头部
     */
    @Test
    void testGetEnabledSuppliers_MissingAuthHeaders() throws Exception {
        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Missing or invalid authentication headers"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试获取所有启用的供应商列表 - 无效的APP ID
     */
    @Test
    void testGetEnabledSuppliers_InvalidAppId() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = generateMD5Signature("invalid_app_id", timestamp, TEST_SECRET_KEY);

        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, "invalid_app_id")
                        .header(HEADER_TIMESTAMP, timestamp)
                        .header(HEADER_SIGNATURE, signature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid APP ID"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试获取所有启用的供应商列表 - 无效的签名
     */
    @Test
    void testGetEnabledSuppliers_InvalidSignature() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String invalidSignature = "invalid_signature";

        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, TEST_APP_ID)
                        .header(HEADER_TIMESTAMP, timestamp)
                        .header(HEADER_SIGNATURE, invalidSignature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid signature"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试获取所有启用的供应商列表 - 过期的时间戳
     */
    @Test
    void testGetEnabledSuppliers_ExpiredTimestamp() throws Exception {
        // 使用10分钟前的时间戳（超过5分钟的允许偏差）
        String expiredTimestamp = String.valueOf((System.currentTimeMillis() / 1000) - 600);
        String signature = generateMD5Signature(TEST_APP_ID, expiredTimestamp, TEST_SECRET_KEY);

        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, TEST_APP_ID)
                        .header(HEADER_TIMESTAMP, expiredTimestamp)
                        .header(HEADER_SIGNATURE, signature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired timestamp"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试获取所有启用的供应商列表 - 无效的时间戳格式
     */
    @Test
    void testGetEnabledSuppliers_InvalidTimestampFormat() throws Exception {
        String invalidTimestamp = "invalid_timestamp";
        String signature = generateMD5Signature(TEST_APP_ID, invalidTimestamp, TEST_SECRET_KEY);

        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, TEST_APP_ID)
                        .header(HEADER_TIMESTAMP, invalidTimestamp)
                        .header(HEADER_SIGNATURE, signature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired timestamp"))
                .andExpect(jsonPath("$.timestamp").exists());
    }



    /**
     * 生成MD5签名
     * 签名算法：MD5(appId + timestamp + secretKey)
     *
     * @param appId     应用ID
     * @param timestamp 时间戳
     * @param secretKey 密钥
     * @return MD5签名
     */
    private String generateMD5Signature(String appId, String timestamp, String secretKey) {
        try {
            String data = appId + timestamp + secretKey;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成MD5签名失败", e);
        }
    }

    /**
     * 创建模拟的供应商配置数据
     *
     * @return 供应商配置列表
     */
    private List<SupplierConfig> createMockSuppliers() {
        SupplierConfig supplier1 = new SupplierConfig();
        supplier1.setId(1L);
        supplier1.setSupplierName("TestSupplier1");
        supplier1.setSupplierCode("TS001");
        supplier1.setApiBaseUrl("https://api.testsupplier1.com");
        supplier1.setSupplierName("test_user1");
        supplier1.setIsActive(true);
        supplier1.setCreatedAt(LocalDateTime.now());
        supplier1.setUpdatedAt(LocalDateTime.now());

        SupplierConfig supplier2 = new SupplierConfig();
        supplier2.setId(2L);
        supplier2.setSupplierName("TestSupplier2");
        supplier2.setSupplierCode("TS002");
        supplier2.setApiBaseUrl("https://api.testsupplier2.com");
        supplier2.setSupplierName("test_user2");
        supplier2.setIsActive(true);
        supplier2.setCreatedAt(LocalDateTime.now());
        supplier2.setUpdatedAt(LocalDateTime.now());

        return Arrays.asList(supplier1, supplier2);
    }

    /**
     * 测试认证成功后的请求处理
     * 验证认证通过后，业务逻辑是否正确执行
     */
    @Test
    void testAuthenticationFlow_Success() throws Exception {
        // 准备测试数据
        List<SupplierConfig> mockSuppliers = createMockSuppliers();
        when(supplierConfigRepository.findByIsActiveTrue()).thenReturn(mockSuppliers);

        // 生成有效的认证头部
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = generateMD5Signature(TEST_APP_ID, timestamp, TEST_SECRET_KEY);

        // 执行请求
        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, TEST_APP_ID)
                        .header(HEADER_TIMESTAMP, timestamp)
                        .header(HEADER_SIGNATURE, signature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").exists())
                .andExpect(jsonPath("$.totalSuppliers").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 测试边界情况：时间戳在允许的偏差范围内
     */
    @Test
    void testTimestampWithinAllowedSkew() throws Exception {
        // 准备测试数据
        List<SupplierConfig> mockSuppliers = createMockSuppliers();
        when(supplierConfigRepository.findByIsActiveTrue()).thenReturn(mockSuppliers);

        // 使用4分钟前的时间戳（在5分钟允许偏差内）
        String timestamp = String.valueOf((System.currentTimeMillis() / 1000) - 240);
        String signature = generateMD5Signature(TEST_APP_ID, timestamp, TEST_SECRET_KEY);

        mockMvc.perform(get("/pax/api/xiwanSupplier/supp/suppliers")
                        .header(HEADER_APP_ID, TEST_APP_ID)
                        .header(HEADER_TIMESTAMP, timestamp)
                        .header(HEADER_SIGNATURE, signature)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").exists());
    }
}
