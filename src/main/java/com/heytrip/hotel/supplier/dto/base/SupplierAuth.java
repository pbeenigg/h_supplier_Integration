package com.heytrip.hotel.supplier.dto.base;


import lombok.Data;
import lombok.Getter;

/**
 * 供应商认证信息DTO
 *
 * @author Pax
 */
@Data
public class SupplierAuth {
    /**
     * {
     *     "appId": "heytrip_supplier_integration_pax",
     *     "secretKey": "HeyTrip@Pax#SupplierIntegration!2025",
     *     "username": "Heytrip_Test",
     *     "password": "Welcome@@123",
     *     "token": "Welcome@@123"
     * }
     */

    /**
     * 应用ID
     */
    private String appId;
    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 令牌
     */
    private String token;





    /**
     * 认证类型：MD5、SHA256、JWT、OAUTH、BasicAuth等
     */
    @Getter
    enum AuthType {
        BasicAuth("BasicAuth","基础认证"),
        BearerToken("BearerToken","令牌认证"),
        OAuth("OAuth","OAuth认证"),
        Jwt("Jwt","JSON Web Token认证"),
        ApiKey("ApiKey","API密钥认证"),
        MD5("MD5","MD5哈希认证"),

        ;

        AuthType(String mode, String description) {
            this.mode = mode;
            this.description = description;
        }

        public static AuthType fromMode(String mode) {
            for (AuthType authMode : AuthType.values()) {
                if (authMode.getMode().equalsIgnoreCase(mode)) {
                    return authMode;
                }
            }
            return null;
        }

        private  String mode;

        private  String description;
    }


}
