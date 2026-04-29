package org.guohai.javasqlweb.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SSF Stream 配置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsfStreamConfig {

    /** Stream ID */
    private String streamId;

    /** Issuer */
    private String issuer;

    /** Audience */
    private List<String> audience;

    /** 投递方式 */
    private String deliveryMethod;

    /** Push 投递端点 URL */
    private String endpointUrl;

    /** 请求的事件类型列表 */
    private List<String> eventsRequested;

    /** 已投递的事件类型列表 */
    private List<String> eventsDelivered;

    /** Stream 状态: enabled / paused / disabled */
    private String status;

    /** 原始配置 (用于展示完整 JSON) */
    private Map<String, Object> rawConfig;
}
