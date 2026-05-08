-- OIDC 配置改造：仅数据库配置，新增 discovery URL 字段，回调地址改为运行时动态计算

ALTER TABLE `oidc_config_tb`
  ADD COLUMN IF NOT EXISTS `openid_configuration_url` VARCHAR(512) NULL COMMENT 'OpenID Discovery URL' AFTER `client_secret`,
  ADD COLUMN IF NOT EXISTS `ssf_configuration_url` VARCHAR(512) NULL COMMENT 'SSF Discovery URL' AFTER `openid_configuration_url`;

-- 将历史 issuer 迁移为标准 discovery URL（仅当新字段为空时）
UPDATE `oidc_config_tb`
SET `openid_configuration_url` = CONCAT(TRIM(TRAILING '/' FROM `issuer`), '/.well-known/openid-configuration')
WHERE (`openid_configuration_url` IS NULL OR `openid_configuration_url` = '')
  AND `issuer` IS NOT NULL
  AND `issuer` <> '';

UPDATE `oidc_config_tb`
SET `ssf_configuration_url` = CONCAT(TRIM(TRAILING '/' FROM `issuer`), '/.well-known/ssf-configuration')
WHERE (`ssf_configuration_url` IS NULL OR `ssf_configuration_url` = '')
  AND `issuer` IS NOT NULL
  AND `issuer` <> '';

-- 新字段强制非空，保证登录可用性判定
ALTER TABLE `oidc_config_tb`
  MODIFY COLUMN `openid_configuration_url` VARCHAR(512) NOT NULL COMMENT 'OpenID Discovery URL',
  MODIFY COLUMN `ssf_configuration_url` VARCHAR(512) NOT NULL COMMENT 'SSF Discovery URL';
