package org.guohai.javasqlweb.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OIDC UserInfo DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OidcUserInfo {

    /** Subject identifier */
    private String sub;

    /** 用户显示名 */
    private String name;

    /** 姓 */
    private String familyName;

    /** 名 */
    private String givenName;

    /** 邮箱 */
    private String email;

    /** 邮箱是否已验证 */
    private Boolean emailVerified;

    /** 首选用户名 */
    private String preferredUsername;
}
