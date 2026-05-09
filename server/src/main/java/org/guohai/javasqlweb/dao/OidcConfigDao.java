package org.guohai.javasqlweb.dao;

import org.apache.ibatis.annotations.*;
import org.guohai.javasqlweb.beans.OidcConfigBean;
import org.springframework.stereotype.Repository;

/**
 * OIDC 配置 DAO
 */
@Repository
public interface OidcConfigDao {

    /**
     * 获取当前 OIDC 配置（仅取第一条）
     */
    @Select("SELECT * FROM oidc_config_tb ORDER BY code LIMIT 1")
    OidcConfigBean getOidcConfig();

    /**
     * 新增 OIDC 配置
     */
    @Insert("INSERT INTO oidc_config_tb " +
            "(client_id, client_secret, openid_configuration_url, ssf_configuration_url, enabled, created_time, updated_time) " +
            "VALUES " +
            "(#{clientId}, #{clientSecret}, #{openidConfigurationUrl}, #{ssfConfigurationUrl}, #{enabled}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "code", keyColumn = "code")
    Boolean insertOidcConfig(OidcConfigBean config);

    /**
     * 更新 OIDC 配置
     */
    @Update("UPDATE oidc_config_tb SET " +
            "client_id = #{clientId}, " +
            "client_secret = #{clientSecret}, " +
            "openid_configuration_url = #{openidConfigurationUrl}, " +
            "ssf_configuration_url = #{ssfConfigurationUrl}, " +
            "enabled = #{enabled}, " +
            "updated_time = NOW() " +
            "WHERE code = #{code}")
    Boolean updateOidcConfig(OidcConfigBean config);

    /**
     * 删除 OIDC 配置
     */
    @Delete("DELETE FROM oidc_config_tb WHERE code = #{code}")
    Boolean deleteOidcConfig(@Param("code") Integer code);

    /**
     * 删除所有 OIDC 配置
     */
    @Delete("DELETE FROM oidc_config_tb")
    Boolean deleteAllOidcConfig();
}
