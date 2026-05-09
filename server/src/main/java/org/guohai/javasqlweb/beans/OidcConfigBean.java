package org.guohai.javasqlweb.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * OIDC 客户端配置 Bean
 * 对应 oidc_config_tb 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OidcConfigBean {

    /** 自增主键 */
    private Integer code;

    /** OIDC Client ID */
    private String clientId;

    /** OIDC Client Secret */
    private String clientSecret;

    /** OpenID Discovery URL */
    private String openidConfigurationUrl;

    /** SSF Discovery URL */
    private String ssfConfigurationUrl;

    /** OIDC 回调地址（动态计算，仅回显） */
    private String callbackUrl;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private Date createdTime;

    /** 更新时间 */
    private Date updatedTime;

    /** 配置来源（非持久化字段，用于前端展示） */
    private transient String configSource;
}
