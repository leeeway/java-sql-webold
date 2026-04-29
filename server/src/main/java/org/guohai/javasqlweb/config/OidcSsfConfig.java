package org.guohai.javasqlweb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OIDC / SSF 配置
 * 从 application.yml 的 oidc 前缀读取配置项。
 * 远端 well-known 端点在 service 层自动发现。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "oidc")
public class OidcSsfConfig {

    /** OIDC Client ID */
    private String clientId;

    /** OIDC Client Secret */
    private String clientSecret;

    /** OIDC Issuer (e.g. https://mdm.gydev.cn) */
    private String issuer;

    /** 本系统的 OIDC 回调地址 */
    private String callbackUrl;
}
