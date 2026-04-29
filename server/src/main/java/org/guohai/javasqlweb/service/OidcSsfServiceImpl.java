package org.guohai.javasqlweb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.guohai.javasqlweb.beans.*;
import org.guohai.javasqlweb.config.OidcSsfConfig;
import org.guohai.javasqlweb.dao.OidcConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OIDC + SSF 服务实现。
 * 使用 Java 17 HttpClient 与远端 OIDC Provider / SSF Transmitter 交互。
 * 令牌与事件存储在内存中 (重启丢失)。
 */
@Service
public class OidcSsfServiceImpl implements OidcSsfService {

    private static final Logger LOG = LoggerFactory.getLogger(OidcSsfServiceImpl.class);

    private final OidcSsfConfig config;
    private final OidcConfigDao oidcConfigDao;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    /** OIDC discovery 缓存 */
    private volatile Map<String, Object> oidcDiscovery;
    /** SSF discovery 缓存 */
    private volatile Map<String, Object> ssfDiscovery;

    /** 存储的令牌 */
    private volatile OidcTokenInfo storedTokens;
    /** 存储的用户信息 */
    private volatile OidcUserInfo storedUserInfo;

    /** PKCE state → code_verifier 映射 */
    private final ConcurrentHashMap<String, String> pkceStore = new ConcurrentHashMap<>();

    /** 事件日志 (最多保留 500 条) */
    private final CopyOnWriteArrayList<SsfEvent> eventLog = new CopyOnWriteArrayList<>();
    private static final int MAX_EVENT_LOG = 500;

    public OidcSsfServiceImpl(OidcSsfConfig config, OidcConfigDao oidcConfigDao, ObjectMapper objectMapper) {
        this.config = config;
        this.oidcConfigDao = oidcConfigDao;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 获取生效的 OIDC 配置：优先数据库，fallback 到 yml。
     */
    private OidcConfigBean getEffectiveConfig() {
        try {
            OidcConfigBean dbConfig = oidcConfigDao.getOidcConfig();
            if (dbConfig != null && Boolean.TRUE.equals(dbConfig.getEnabled())) {
                dbConfig.setConfigSource("database");
                return dbConfig;
            }
        } catch (Exception e) {
            LOG.warn("Failed to load OIDC config from DB, falling back to yml", e);
        }
        // fallback 到 application.yml
        OidcConfigBean fallback = OidcConfigBean.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .issuer(config.getIssuer())
                .callbackUrl(config.getCallbackUrl())
                .enabled(true)
                .configSource("yml")
                .build();
        return fallback;
    }

    // ════════════════════════════════════════════════════════
    //  OIDC Discovery
    // ════════════════════════════════════════════════════════

    private Map<String, Object> getOidcDiscovery() {
        if (oidcDiscovery != null) {
            return oidcDiscovery;
        }
        synchronized (this) {
            if (oidcDiscovery != null) {
                return oidcDiscovery;
            }
            try {
                String url = getEffectiveConfig().getIssuer() + "/.well-known/openid-configuration";
                oidcDiscovery = httpGetJson(url);
                LOG.info("OIDC discovery loaded from {}", url);
            } catch (Exception e) {
                LOG.error("Failed to load OIDC discovery", e);
                oidcDiscovery = Map.of();
            }
        }
        return oidcDiscovery;
    }

    private Map<String, Object> getSsfDiscovery() {
        if (ssfDiscovery != null) {
            return ssfDiscovery;
        }
        synchronized (this) {
            if (ssfDiscovery != null) {
                return ssfDiscovery;
            }
            try {
                String url = getEffectiveConfig().getIssuer() + "/.well-known/ssf-configuration";
                ssfDiscovery = httpGetJson(url);
                LOG.info("SSF discovery loaded from {}", url);
            } catch (Exception e) {
                LOG.error("Failed to load SSF discovery", e);
                ssfDiscovery = Map.of();
            }
        }
        return ssfDiscovery;
    }

    private String disc(String key) {
        Object v = getOidcDiscovery().get(key);
        return v != null ? v.toString() : "";
    }

    private String ssfDisc(String key) {
        Object v = getSsfDiscovery().get(key);
        return v != null ? v.toString() : "";
    }

    // ════════════════════════════════════════════════════════
    //  OIDC Authorization
    // ════════════════════════════════════════════════════════

    @Override
    public Map<String, String> buildAuthorizationUrl() {
        String state = generateRandomString(32);
        String codeVerifier = generateRandomString(64);
        String codeChallenge = computeS256Challenge(codeVerifier);

        pkceStore.put(state, codeVerifier);

        OidcConfigBean effectiveConfig = getEffectiveConfig();
        String authEndpoint = disc("authorization_endpoint");
        String scopes = "openid email profile ssf.manage ssf.read";

        String url = authEndpoint
                + "?response_type=code"
                + "&client_id=" + enc(effectiveConfig.getClientId())
                + "&redirect_uri=" + enc(effectiveConfig.getCallbackUrl())
                + "&scope=" + enc(scopes)
                + "&state=" + enc(state)
                + "&code_challenge=" + enc(codeChallenge)
                + "&code_challenge_method=S256";

        return Map.of("authUrl", url, "state", state);
    }

    @Override
    public Result<OidcTokenInfo> exchangeCodeForTokens(String code, String state) {
        String codeVerifier = pkceStore.remove(state);
        if (codeVerifier == null) {
            return new Result<>(false, "Invalid state parameter", null);
        }

        OidcConfigBean effectiveConfig = getEffectiveConfig();
        String tokenEndpoint = disc("token_endpoint");
        String body = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(effectiveConfig.getCallbackUrl())
                + "&client_id=" + enc(effectiveConfig.getClientId())
                + "&client_secret=" + enc(effectiveConfig.getClientSecret())
                + "&code_verifier=" + enc(codeVerifier);

        try {
            Map<String, Object> tokenResponse = httpPostForm(tokenEndpoint, body);
            OidcTokenInfo tokens = parseTokenResponse(tokenResponse);
            storedTokens = tokens;

            // 自动获取 userinfo
            try {
                fetchAndStoreUserInfo(tokens.getAccessToken());
            } catch (Exception e) {
                LOG.warn("Failed to fetch userinfo after token exchange", e);
            }

            return new Result<>(true, "OK", maskTokenInfo(tokens));
        } catch (Exception e) {
            LOG.error("Token exchange failed", e);
            return new Result<>(false, "Token exchange failed: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<OidcTokenInfo> refreshTokens() {
        if (storedTokens == null || storedTokens.getRefreshToken() == null) {
            return new Result<>(false, "No refresh token available", null);
        }

        OidcConfigBean effectiveConfig = getEffectiveConfig();
        String tokenEndpoint = disc("token_endpoint");
        String body = "grant_type=refresh_token"
                + "&refresh_token=" + enc(storedTokens.getRefreshToken())
                + "&client_id=" + enc(effectiveConfig.getClientId())
                + "&client_secret=" + enc(effectiveConfig.getClientSecret());

        try {
            Map<String, Object> tokenResponse = httpPostForm(tokenEndpoint, body);
            OidcTokenInfo tokens = parseTokenResponse(tokenResponse);
            storedTokens = tokens;

            try {
                fetchAndStoreUserInfo(tokens.getAccessToken());
            } catch (Exception e) {
                LOG.warn("Failed to fetch userinfo after refresh", e);
            }

            return new Result<>(true, "Tokens refreshed", maskTokenInfo(tokens));
        } catch (Exception e) {
            LOG.error("Token refresh failed", e);
            return new Result<>(false, "Token refresh failed: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<OidcUserInfo> getUserInfo() {
        if (storedUserInfo != null) {
            return new Result<>(true, "OK", storedUserInfo);
        }
        if (storedTokens == null) {
            return new Result<>(false, "Not connected", null);
        }
        try {
            fetchAndStoreUserInfo(storedTokens.getAccessToken());
            return new Result<>(true, "OK", storedUserInfo);
        } catch (Exception e) {
            return new Result<>(false, "Failed to fetch userinfo: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<OidcTokenInfo> getStatus() {
        if (storedTokens == null) {
            OidcTokenInfo disconnected = OidcTokenInfo.builder().connected(false).build();
            return new Result<>(true, "Disconnected", disconnected);
        }
        return new Result<>(true, "Connected", maskTokenInfo(storedTokens));
    }

    @Override
    public Result<String> disconnect() {
        storedTokens = null;
        storedUserInfo = null;
        pkceStore.clear();
        return new Result<>(true, "Disconnected", null);
    }

    // ════════════════════════════════════════════════════════
    //  SSF Stream Management
    // ════════════════════════════════════════════════════════

    @Override
    public Result<SsfStreamConfig> getSsfStream() {
        if (storedTokens == null) {
            return new Result<>(false, "Not connected to OIDC provider", null);
        }
        String endpoint = ssfDisc("configuration_endpoint");
        try {
            Map<String, Object> response = httpGetJsonAuth(endpoint, storedTokens.getAccessToken());
            return new Result<>(true, "OK", parseSsfStreamConfig(response));
        } catch (Exception e) {
            LOG.error("Failed to get SSF stream", e);
            return new Result<>(false, "Failed to get SSF stream: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<SsfStreamConfig> createSsfStream(String endpointUrl, List<String> eventsRequested) {
        if (storedTokens == null) {
            return new Result<>(false, "Not connected to OIDC provider", null);
        }
        String endpoint = ssfDisc("configuration_endpoint");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        Map<String, Object> delivery = new LinkedHashMap<>();
        delivery.put("method", "https://schemas.openid.net/secevent/risc/delivery-method/push");
        delivery.put("endpoint_url", endpointUrl);
        requestBody.put("delivery", delivery);
        if (eventsRequested != null && !eventsRequested.isEmpty()) {
            requestBody.put("events_requested", eventsRequested);
        }

        try {
            String json = objectMapper.writeValueAsString(requestBody);
            Map<String, Object> response = httpPostJsonAuth(endpoint, json, storedTokens.getAccessToken());
            return new Result<>(true, "Stream created", parseSsfStreamConfig(response));
        } catch (Exception e) {
            LOG.error("Failed to create SSF stream", e);
            return new Result<>(false, "Failed to create SSF stream: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<SsfStreamConfig> updateSsfStream(String status, List<String> eventsRequested) {
        if (storedTokens == null) {
            return new Result<>(false, "Not connected to OIDC provider", null);
        }
        String endpoint = ssfDisc("configuration_endpoint");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        if (status != null) {
            requestBody.put("status", status);
        }
        if (eventsRequested != null) {
            requestBody.put("events_requested", eventsRequested);
        }

        try {
            String json = objectMapper.writeValueAsString(requestBody);
            Map<String, Object> response = httpPatchJsonAuth(endpoint, json, storedTokens.getAccessToken());
            return new Result<>(true, "Stream updated", parseSsfStreamConfig(response));
        } catch (Exception e) {
            LOG.error("Failed to update SSF stream", e);
            return new Result<>(false, "Failed to update SSF stream: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<String> deleteSsfStream() {
        if (storedTokens == null) {
            return new Result<>(false, "Not connected to OIDC provider", null);
        }
        String endpoint = ssfDisc("configuration_endpoint");
        try {
            httpDeleteAuth(endpoint, storedTokens.getAccessToken());
            return new Result<>(true, "Stream deleted", null);
        } catch (Exception e) {
            LOG.error("Failed to delete SSF stream", e);
            return new Result<>(false, "Failed to delete SSF stream: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<String> requestVerification() {
        if (storedTokens == null) {
            return new Result<>(false, "Not connected to OIDC provider", null);
        }
        String endpoint = ssfDisc("verification_endpoint");
        try {
            Map<String, Object> requestBody = Map.of("state", generateRandomString(16));
            String json = objectMapper.writeValueAsString(requestBody);
            httpPostJsonAuth(endpoint, json, storedTokens.getAccessToken());
            return new Result<>(true, "Verification requested", null);
        } catch (Exception e) {
            LOG.error("Failed to request verification", e);
            return new Result<>(false, "Failed to request verification: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<String> receiveSsfEvent(String setToken) {
        try {
            // SET 是一个 JWT，解析 payload (不做签名验证，仅展示用)
            String[] parts = setToken.split("\\.");
            if (parts.length < 2) {
                return new Result<>(false, "Invalid SET format", null);
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});

            SsfEvent event = SsfEvent.builder()
                    .jti(strVal(payload, "jti"))
                    .iss(strVal(payload, "iss"))
                    .aud(strVal(payload, "aud"))
                    .receivedAt(Instant.now())
                    .rawPayload(payload)
                    .build();

            // 解析 iat
            Object iatObj = payload.get("iat");
            if (iatObj instanceof Number) {
                event.setIat(Instant.ofEpochSecond(((Number) iatObj).longValue()));
            }

            // 解析 events
            @SuppressWarnings("unchecked")
            Map<String, Object> events = (Map<String, Object>) payload.get("events");
            if (events != null && !events.isEmpty()) {
                String eventType = events.keySet().iterator().next();
                event.setEventType(eventType);

                @SuppressWarnings("unchecked")
                Map<String, Object> eventData = (Map<String, Object>) events.get(eventType);
                if (eventData != null && eventData.containsKey("subject")) {
                    event.setSubject(objectMapper.writeValueAsString(eventData.get("subject")));
                }
            }

            eventLog.add(0, event);
            while (eventLog.size() > MAX_EVENT_LOG) {
                eventLog.remove(eventLog.size() - 1);
            }

            LOG.info("Received SSF event: type={}, jti={}", event.getEventType(), event.getJti());
            return new Result<>(true, "Event received", null);
        } catch (Exception e) {
            LOG.error("Failed to process SSF event", e);
            return new Result<>(false, "Failed to process event: " + e.getMessage(), null);
        }
    }

    @Override
    public List<SsfEvent> getEventLog() {
        return List.copyOf(eventLog);
    }

    // ════════════════════════════════════════════════════════
    //  OIDC Config Management
    // ════════════════════════════════════════════════════════

    @Override
    public Result<OidcConfigBean> getOidcConfig() {
        OidcConfigBean effective = getEffectiveConfig();
        // 脱敏 secret
        if (effective.getClientSecret() != null && effective.getClientSecret().length() > 8) {
            effective.setClientSecret(
                    effective.getClientSecret().substring(0, 4) + "****"
                            + effective.getClientSecret().substring(effective.getClientSecret().length() - 4));
        }
        return new Result<>(true, "OK", effective);
    }

    @Override
    public Result<OidcConfigBean> saveOidcConfig(OidcConfigBean incoming) {
        try {
            OidcConfigBean existing = oidcConfigDao.getOidcConfig();
            if (existing != null) {
                // 如果前端传的是脱敏 secret（含 ****），保留原值
                if (incoming.getClientSecret() != null && incoming.getClientSecret().contains("****")) {
                    incoming.setClientSecret(existing.getClientSecret());
                }
                incoming.setCode(existing.getCode());
                oidcConfigDao.updateOidcConfig(incoming);
            } else {
                if (incoming.getEnabled() == null) {
                    incoming.setEnabled(true);
                }
                oidcConfigDao.insertOidcConfig(incoming);
            }
            // 清除 discovery 缓存以重新加载
            clearDiscoveryCache();

            OidcConfigBean saved = oidcConfigDao.getOidcConfig();
            saved.setConfigSource("database");
            // 脱敏返回
            if (saved.getClientSecret() != null && saved.getClientSecret().length() > 8) {
                saved.setClientSecret(
                        saved.getClientSecret().substring(0, 4) + "****"
                                + saved.getClientSecret().substring(saved.getClientSecret().length() - 4));
            }
            return new Result<>(true, "配置已保存", saved);
        } catch (Exception e) {
            LOG.error("Failed to save OIDC config", e);
            return new Result<>(false, "保存失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<String> deleteOidcConfig() {
        try {
            oidcConfigDao.deleteAllOidcConfig();
            clearDiscoveryCache();
            return new Result<>(true, "配置已删除，将回退到 yml 配置", null);
        } catch (Exception e) {
            LOG.error("Failed to delete OIDC config", e);
            return new Result<>(false, "删除失败: " + e.getMessage(), null);
        }
    }

    @Override
    public Result<Map<String, Object>> testOidcConnection(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            return new Result<>(false, "Issuer URL 不能为空", null);
        }
        String url = issuer.replaceAll("/+$", "") + "/.well-known/openid-configuration";
        try {
            Map<String, Object> discovery = httpGetJson(url);
            if (discovery.containsKey("authorization_endpoint")) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("issuer", discovery.get("issuer"));
                summary.put("authorization_endpoint", discovery.get("authorization_endpoint"));
                summary.put("token_endpoint", discovery.get("token_endpoint"));
                summary.put("userinfo_endpoint", discovery.get("userinfo_endpoint"));
                return new Result<>(true, "连接成功", summary);
            }
            return new Result<>(false, "返回的 discovery 文档缺少 authorization_endpoint", null);
        } catch (Exception e) {
            return new Result<>(false, "连接失败: " + e.getMessage(), null);
        }
    }

    private void clearDiscoveryCache() {
        this.oidcDiscovery = null;
        this.ssfDiscovery = null;
    }

    // ════════════════════════════════════════════════════════
    //  Internal Helpers
    // ════════════════════════════════════════════════════════

    private void fetchAndStoreUserInfo(String accessToken) throws IOException, InterruptedException {
        String userinfoEndpoint = disc("userinfo_endpoint");
        Map<String, Object> data = httpGetJsonAuth(userinfoEndpoint, accessToken);
        storedUserInfo = OidcUserInfo.builder()
                .sub(strVal(data, "sub"))
                .name(strVal(data, "name"))
                .familyName(strVal(data, "family_name"))
                .givenName(strVal(data, "given_name"))
                .email(strVal(data, "email"))
                .emailVerified(boolVal(data, "email_verified"))
                .preferredUsername(strVal(data, "preferred_username"))
                .build();
    }

    private OidcTokenInfo parseTokenResponse(Map<String, Object> response) {
        String accessToken = strVal(response, "access_token");
        String idToken = strVal(response, "id_token");
        String refreshToken = strVal(response, "refresh_token");
        String tokenType = strVal(response, "token_type");
        String scope = strVal(response, "scope");

        int expiresIn = 3600;
        Object exp = response.get("expires_in");
        if (exp instanceof Number) {
            expiresIn = ((Number) exp).intValue();
        }

        List<String> scopes = scope != null && !scope.isEmpty()
                ? Arrays.asList(scope.split("\\s+"))
                : List.of();

        return OidcTokenInfo.builder()
                .connected(true)
                .accessToken(accessToken)
                .idToken(idToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .expiresAt(Instant.now().plusSeconds(expiresIn))
                .scopes(scopes)
                .build();
    }

    private OidcTokenInfo maskTokenInfo(OidcTokenInfo tokens) {
        return OidcTokenInfo.builder()
                .connected(tokens.isConnected())
                .accessToken(mask(tokens.getAccessToken()))
                .idToken(mask(tokens.getIdToken()))
                .refreshToken(null) // 不返回 refresh token 给前端
                .tokenType(tokens.getTokenType())
                .expiresAt(tokens.getExpiresAt())
                .scopes(tokens.getScopes())
                .build();
    }

    private String mask(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }

    @SuppressWarnings("unchecked")
    private SsfStreamConfig parseSsfStreamConfig(Map<String, Object> data) {
        SsfStreamConfig cfg = SsfStreamConfig.builder()
                .streamId(strVal(data, "stream_id"))
                .issuer(strVal(data, "iss"))
                .status(strVal(data, "status"))
                .rawConfig(data)
                .build();

        Object aud = data.get("aud");
        if (aud instanceof List) {
            cfg.setAudience((List<String>) aud);
        } else if (aud instanceof String) {
            cfg.setAudience(List.of((String) aud));
        }

        Object delivery = data.get("delivery");
        if (delivery instanceof Map) {
            Map<String, Object> dm = (Map<String, Object>) delivery;
            cfg.setDeliveryMethod(strVal(dm, "method"));
            cfg.setEndpointUrl(strVal(dm, "endpoint_url"));
        }

        Object evReq = data.get("events_requested");
        if (evReq instanceof List) {
            cfg.setEventsRequested((List<String>) evReq);
        }

        Object evDel = data.get("events_delivered");
        if (evDel instanceof List) {
            cfg.setEventsDelivered((List<String>) evDel);
        }

        return cfg;
    }

    // ── HTTP helpers ───────────────────────────────────────

    private Map<String, Object> httpGetJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    private Map<String, Object> httpGetJsonAuth(String url, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    private Map<String, Object> httpPostForm(String url, String formBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Token endpoint returned " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    private Map<String, Object> httpPostJsonAuth(String url, String jsonBody, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        String body = response.body();
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(body, new TypeReference<>() {});
    }

    private Map<String, Object> httpPatchJsonAuth(String url, String jsonBody, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        String body = response.body();
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(body, new TypeReference<>() {});
    }

    private void httpDeleteAuth(String url, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    // ── Crypto helpers ─────────────────────────────────────

    private static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length);
    }

    private static String computeS256Challenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String strVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static Boolean boolVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return null;
    }
}
