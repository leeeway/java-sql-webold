-- 用户表增加 OIDC Subject 关联字段
ALTER TABLE `user_tb`
  ADD COLUMN `oidc_sub` VARCHAR(256) NULL COMMENT 'OIDC Subject 标识' AFTER `account_status`,
  ADD UNIQUE KEY `uk_oidc_sub` (`oidc_sub`);
