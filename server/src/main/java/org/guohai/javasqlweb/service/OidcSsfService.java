package org.guohai.javasqlweb.service;

import org.guohai.javasqlweb.beans.*;

import java.util.List;
import java.util.Map;

/**
 * OIDC + SSF 服务接口
 */
public interface OidcSsfService {

    // ── OIDC ───────────────────────────────────────────

    /**
     * 构建 OIDC 授权 URL (含 PKCE)
     * @return 包含 authUrl 和 state 的 Map
     */
    Map<String, String> buildAuthorizationUrl();

    /**
     * 用授权码换取令牌
     * @param code 授权码
     * @param state 状态参数(用于查找 code_verifier)
     * @return 令牌信息
     */
    Result<OidcTokenInfo> exchangeCodeForTokens(String code, String state);

    /**
     * 刷新令牌
     * @return 新的令牌信息
     */
    Result<OidcTokenInfo> refreshTokens();

    /**
     * 获取 UserInfo
     * @return 用户信息
     */
    Result<OidcUserInfo> getUserInfo();

    /**
     * 获取当前连接状态
     * @return 令牌信息(脱敏)
     */
    Result<OidcTokenInfo> getStatus();

    /**
     * 断开连接 (清除存储的令牌)
     * @return 结果
     */
    Result<String> disconnect();

    // ── SSF ────────────────────────────────────────────

    /**
     * 获取当前 SSF stream 配置
     * @return stream 配置
     */
    Result<SsfStreamConfig> getSsfStream();

    /**
     * 创建 SSF stream
     * @param endpointUrl 推送端点 URL
     * @param eventsRequested 请求的事件类型
     * @return 创建后的 stream 配置
     */
    Result<SsfStreamConfig> createSsfStream(String endpointUrl, List<String> eventsRequested);

    /**
     * 更新 SSF stream
     * @param status 新状态
     * @param eventsRequested 新事件列表
     * @return 更新后的 stream 配置
     */
    Result<SsfStreamConfig> updateSsfStream(String status, List<String> eventsRequested);

    /**
     * 删除 SSF stream
     * @return 结果
     */
    Result<String> deleteSsfStream();

    /**
     * 请求验证事件
     * @return 结果
     */
    Result<String> requestVerification();

    /**
     * 接收 SSF push 事件 (SET)
     * @param setToken SET JWT 字符串
     * @return 结果
     */
    Result<String> receiveSsfEvent(String setToken);

    /**
     * 获取事件日志
     * @return 事件列表
     */
    List<SsfEvent> getEventLog();

    // ── OIDC Config ─────────────────────────────────────

    /**
     * 获取当前 OIDC 配置（secret 脱敏）
     * @return 配置信息
     */
    Result<OidcConfigBean> getOidcConfig();

    /**
     * 保存 OIDC 配置（新增或更新）
     * @param config 配置
     * @return 保存后的配置
     */
    Result<OidcConfigBean> saveOidcConfig(OidcConfigBean config);

    /**
     * 删除 OIDC 配置
     * @return 结果
     */
    Result<String> deleteOidcConfig();

    /**
     * 测试 OIDC Provider 连通性
     * @param issuer Issuer URL
     * @return 测试结果
     */
    Result<Map<String, Object>> testOidcConnection(String issuer);

    // ── OIDC Login ──────────────────────────────────────

    /**
     * 检查 OIDC 登录是否可用
     * @return 是否可用
     */
    boolean isOidcLoginEnabled();

    /**
     * 构建登录用的 OIDC 授权 URL
     * @return 包含 authUrl 和 state 的 Map
     */
    Map<String, String> buildLoginAuthorizationUrl();

    /**
     * 处理 OIDC 登录回调：换令牌 → 取 userinfo → 查找/创建用户 → 签发 token
     * @param code 授权码
     * @param state 状态参数
     * @return 包含 token 的用户信息
     */
    Result<UserBean> handleLoginCallback(String code, String state);
}
