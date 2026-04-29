package org.guohai.javasqlweb.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * SSF 事件 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsfEvent {

    /** JWT ID (jti) */
    private String jti;

    /** Issuer */
    private String iss;

    /** Issued At */
    private Instant iat;

    /** Audience */
    private String aud;

    /** 事件类型 URI */
    private String eventType;

    /** 事件主体描述 */
    private String subject;

    /** 原始 SET payload */
    private Map<String, Object> rawPayload;

    /** 接收时间 */
    private Instant receivedAt;
}
