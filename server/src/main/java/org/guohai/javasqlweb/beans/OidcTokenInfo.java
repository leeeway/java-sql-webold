package org.guohai.javasqlweb.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * OIDC 令牌信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OidcTokenInfo {

    /** 是否已连接 */
    private boolean connected;

    /** Access Token (返回给前端时脱敏) */
    private String accessToken;

    /** ID Token (返回给前端时脱敏) */
    private String idToken;

    /** Refresh Token (仅后端使用, 不返回前端) */
    private String refreshToken;

    /** Access Token 过期时间 */
    private Instant expiresAt;

    /** 已授权 scopes */
    private List<String> scopes;

    /** Token 类型 */
    private String tokenType;
}
