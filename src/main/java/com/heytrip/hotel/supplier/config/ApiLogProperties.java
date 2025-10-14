package com.heytrip.hotel.supplier.config;

import com.heytrip.hotel.supplier.utils.JsonCompressionUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * API日志配置属性类
 *
 * @author HeyTrip
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "app.api-log")
@Data
public class ApiLogProperties {

    private boolean enabled = true;
    private int maxContentLength = 10000;
    private Compression compression = new Compression();


    @Data
    public static class Compression {
        private boolean enabled = true;
        private int threshold = 5000;
        private String algorithm = "gzip";
        private int level = 6;

        public JsonCompressionUtil.CompressionAlgorithm getCompressionAlgorithm() {
            return JsonCompressionUtil.CompressionAlgorithm.fromString(algorithm);
        }
    }


}
