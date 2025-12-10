package com.heytrip.hotel.supplier.adapter.parser;

import com.heytrip.hotel.supplier.utils.CoordinateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AOStaticDataParser 经纬度解析功能测试
 * 验证10种异常情况的处理：
 * 1. 空值或格式不正确
 * 2. 超出合理范围
 * 3. 值为0
 * 4. 小数点后过多位数
 * 5. 负数
 * 6. 科学计数法
 * 7. 前后空格
 * 8. 中文符号
 * 9. 特殊符号
 * 10. 多余字符
 */
class AOStaticDataParserCoordinateTest {

    private CoordinateUtil parser;

    @BeforeEach
    void setUp() {
        parser = new CoordinateUtil();
    }

    /**
     * 调用私有方法parseCoordinate进行测试
     */
    private String parseCoordinate(String rawValue, String coordType, double minValue, double maxValue, String hotelCode) {
        return (String) ReflectionTestUtils.invokeMethod(parser, "parseCoordinate", rawValue, coordType, minValue, maxValue, hotelCode);
    }

    @Test
    void testValidCoordinates() {
        // 正常的经纬度
        assertEquals("3.162790", parseCoordinate("3.162790", "纬度", -90.0, 90.0, "TEST001"));
        assertEquals("101.711120", parseCoordinate("101.711120", "经度", -180.0, 180.0, "TEST001"));
        
        // 负数经纬度
        assertEquals("-3.162790", parseCoordinate("-3.162790", "纬度", -90.0, 90.0, "TEST002"));
        assertEquals("-101.711120", parseCoordinate("-101.711120", "经度", -180.0, 180.0, "TEST002"));
        
        // 带前导空格的负数
        assertEquals("-3.162790", parseCoordinate("  -3.162790", "纬度", -90.0, 90.0, "TEST002"));
        assertEquals("-101.711120", parseCoordinate("  -101.711120  ", "经度", -180.0, 180.0, "TEST002"));
        
        // 多个连续负号统一为单个负号（容错处理）
        assertEquals("-3.162790", parseCoordinate("  --3.162790", "纬度", -90.0, 90.0, "TEST002"));
        assertEquals("-101.711120", parseCoordinate("  --101.711120  ", "经度", -180.0, 180.0, "TEST002"));
        assertEquals("-3.162790", parseCoordinate("---3.162790", "纬度", -90.0, 90.0, "TEST002"));
        assertEquals("-3.162790", parseCoordinate("----3.162790", "纬度", -90.0, 90.0, "TEST002"));
    }

    @Test
    void testNullAndEmpty() {
        // 空值返回空字符串
        assertEquals("", parseCoordinate(null, "纬度", -90.0, 90.0, "TEST003"));
        assertEquals("", parseCoordinate("", "纬度", -90.0, 90.0, "TEST003"));
        assertEquals("", parseCoordinate("   ", "纬度", -90.0, 90.0, "TEST003"));
    }

    @Test
    void testOutOfRange() {
        // 纬度超出范围 [-90, 90]
        assertEquals("", parseCoordinate("91.0", "纬度", -90.0, 90.0, "TEST004"));
        assertEquals("", parseCoordinate("-91.0", "纬度", -90.0, 90.0, "TEST004"));
        
        // 经度超出范围 [-180, 180]
        assertEquals("", parseCoordinate("181.0", "经度", -180.0, 180.0, "TEST004"));
        assertEquals("", parseCoordinate("-181.0", "经度", -180.0, 180.0, "TEST004"));
    }

    @Test
    void testZeroValue() {
        // 0值应该被过滤
        assertEquals("", parseCoordinate("0", "纬度", -90.0, 90.0, "TEST005"));
        assertEquals("", parseCoordinate("0.0", "纬度", -90.0, 90.0, "TEST005"));
        assertEquals("", parseCoordinate("0.000000", "纬度", -90.0, 90.0, "TEST005"));
    }

    @Test
    void testTooManyDecimals() {
        // 小数点过多，应该被格式化为6位
        String result = parseCoordinate("3.1627901234567890", "纬度", -90.0, 90.0, "TEST006");
        assertNotNull(result);
        assertEquals("3.162790", result);
    }

    @Test
    void testScientificNotation() {
        // 科学计数法
        String result = parseCoordinate("3.162790e0", "纬度", -90.0, 90.0, "TEST007");
        assertNotNull(result);
        assertEquals("3.162790", result);
        
        // 1.01711E2 = 1.01711 × 10² = 101.711
        result = parseCoordinate("1.01711E2", "经度", -180.0, 180.0, "TEST007");
        assertNotNull(result);
        // 1.01711E2实际等于101.711
        assertEquals("101.711000", result);
    }

    @Test
    void testWithSpaces() {
        // 前后空格
        assertEquals("3.162790", parseCoordinate("  3.162790  ", "纬度", -90.0, 90.0, "TEST008"));
        assertEquals("101.711120", parseCoordinate("\t101.711120\n", "经度", -180.0, 180.0, "TEST008"));
    }

    @Test
    void testChineseSymbols() {
        // 中文小数点
        assertEquals("3.162790", parseCoordinate("3。162790", "纬度", -90.0, 90.0, "TEST009"));
        // 中文逗号作为分隔符（取第一个数字）
        assertEquals("101.000000", parseCoordinate("101，711120", "经度", -180.0, 180.0, "TEST009"));
    }

    @Test
    void testSpecialSymbols() {
        // HTML实体符号
        assertEquals("3.084800", parseCoordinate("3.0848&deg; N", "纬度", -90.0, 90.0, "TEST010"));
        assertEquals("101.673300", parseCoordinate("101.6733&deg; E", "经度", -180.0, 180.0, "TEST010"));
        
        // 度分秒符号
        assertEquals("3.084800", parseCoordinate("3.0848° N", "纬度", -90.0, 90.0, "TEST010"));
        assertEquals("101.673300", parseCoordinate("101.6733' E", "经度", -180.0, 180.0, "TEST010"));
    }

    @Test
    void testExtraCharacters() {
        // 带逗号分隔的多个数字，只取第一个
        assertEquals("3.162790", parseCoordinate("3.162790, 101.711120,17", "纬度", -90.0, 90.0, "TEST011"));
        assertEquals("101.711120", parseCoordinate("101.711120, 3.162790", "经度", -180.0, 180.0, "TEST011"));
        
        // 多余逗号
        assertEquals("3.081708", parseCoordinate("3.0817076 ,101.5599172,", "纬度", -90.0, 90.0, "TEST011"));
    }

    @Test
    void testComplexCases() {
        // 复杂综合案例
        assertEquals("3.084800", parseCoordinate("  3.0848&deg; N , ", "纬度", -90.0, 90.0, "TEST012"));
        // 以逗号开头的情况，取第一个有效数字
        assertEquals("101.673300", parseCoordinate(" , 101.6733&deg; E  ", "经度", -180.0, 180.0, "TEST012"));
        
        // 多个小数点
        assertEquals("3.162790", parseCoordinate("3.162.790", "纬度", -90.0, 90.0, "TEST013"));
    }

    @Test
    void testInvalidFormats() {
        // 无法解析的格式返回空字符串
        assertEquals("", parseCoordinate("abc", "纬度", -90.0, 90.0, "TEST014"));
        assertEquals("", parseCoordinate("N/A", "纬度", -90.0, 90.0, "TEST014"));
        assertEquals("", parseCoordinate("无效数据", "纬度", -90.0, 90.0, "TEST014"));
        
        // 负号在中间位置的异常格式
        assertEquals("", parseCoordinate("3.-162790", "纬度", -90.0, 90.0, "TEST015"));
        assertEquals("", parseCoordinate("3-.162790", "经度", -180.0, 180.0, "TEST015"));
    }
    
    @Test
    void testLatLonSwapDetection() {
        // 检测经纬度互换：纬度值在91-180之间，可能是经度值
        assertEquals("", parseCoordinate("101.711120", "纬度", -90.0, 90.0, "TEST016"));
        assertEquals("", parseCoordinate("-101.711120", "纬度", -90.0, 90.0, "TEST016"));
        
        // 经度值在纬度范围内是正常的
        assertEquals("3.162790", parseCoordinate("3.162790", "经度", -180.0, 180.0, "TEST016"));
    }
    
    @Test
    void testEdgeCases() {
        // 极小值（不为0但接近0）
        assertEquals("0.000002", parseCoordinate("0.000002", "纬度", -90.0, 90.0, "TEST017"));
        
        // 边界值测试
        assertEquals("90.000000", parseCoordinate("90", "纬度", -90.0, 90.0, "TEST017"));
        assertEquals("-90.000000", parseCoordinate("-90", "纬度", -90.0, 90.0, "TEST017"));
        assertEquals("180.000000", parseCoordinate("180", "经度", -180.0, 180.0, "TEST017"));
        assertEquals("-180.000000", parseCoordinate("-180", "经度", -180.0, 180.0, "TEST017"));
    }
}
