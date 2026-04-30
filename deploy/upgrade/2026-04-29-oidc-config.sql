-- OIDC 配置表
-- 存储 OIDC Provider 的客户端配置，替代 application.yml 硬编码

CREATE TABLE IF NOT EXISTS `oidc_config_tb` (
  `code`           INT          NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `client_id`      VARCHAR(256) NOT NULL COMMENT 'OIDC Client ID',
  `client_secret`  VARCHAR(512) NOT NULL COMMENT 'OIDC Client Secret',
  `issuer`         VARCHAR(512) NOT NULL COMMENT 'OIDC Issuer URL',
  `callback_url`   VARCHAR(512) NOT NULL COMMENT 'OIDC 回调地址',
  `enabled`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
  `created_time`   DATETIME     NOT NULL COMMENT '创建时间',
  `updated_time`   DATETIME     NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
