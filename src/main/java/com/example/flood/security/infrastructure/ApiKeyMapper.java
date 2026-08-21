package com.example.flood.security.infrastructure;

import com.example.flood.security.application.ApiKeyCredential;
import java.time.Instant;
import java.util.Optional;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApiKeyMapper {

    @ConstructorArgs({
        @Arg(column = "client_id", javaType = long.class),
        @Arg(column = "api_key_id", javaType = long.class),
        @Arg(column = "client_code", javaType = String.class),
        @Arg(column = "key_prefix", javaType = String.class),
        @Arg(column = "secret_hash", javaType = byte[].class),
        @Arg(column = "scopes", javaType = java.util.Set.class, typeHandler = StringSetJsonTypeHandler.class),
        @Arg(column = "key_status", javaType = String.class),
        @Arg(column = "client_status", javaType = String.class),
        @Arg(column = "expires_at", javaType = Instant.class),
        @Arg(column = "last_used_at", javaType = Instant.class),
        @Arg(column = "rate_limit_per_minute", javaType = int.class),
        @Arg(column = "allowed_ips", javaType = java.util.List.class, typeHandler = StringListJsonTypeHandler.class)
    })
    @Select("""
        SELECT c.id client_id, k.id api_key_id, c.client_code, k.key_prefix,
               k.secret_hash, k.scopes, k.status key_status, c.status client_status,
               k.expires_at, k.last_used_at, c.rate_limit_per_minute, c.allowed_ips
        FROM api_key k JOIN api_client c ON c.id = k.client_id
        WHERE k.key_prefix = #{prefix}
        LIMIT 1
        """)
    Optional<ApiKeyCredential> findCredentialByPrefix(@Param("prefix") String prefix);

    @Update("""
        UPDATE api_key SET last_used_at = #{now}
        WHERE id = #{id} AND (last_used_at IS NULL OR last_used_at <= #{cutoff})
        """)
    int touchLastUsed(@Param("id") long id, @Param("cutoff") Instant cutoff, @Param("now") Instant now);

    @Update("""
        INSERT INTO api_key (client_id, key_prefix, secret_hash, scopes, status, expires_at)
        VALUES (#{clientId}, #{prefix}, #{hash}, CAST(#{scopesJson} AS JSON), 'ACTIVE', #{expiresAt})
        ON DUPLICATE KEY UPDATE client_id = VALUES(client_id), secret_hash = VALUES(secret_hash),
          scopes = VALUES(scopes), status = 'ACTIVE', expires_at = VALUES(expires_at), revoked_at = NULL
        """)
    int upsert(@Param("clientId") long clientId, @Param("prefix") String prefix,
               @Param("hash") byte[] hash, @Param("scopesJson") String scopesJson,
               @Param("expiresAt") Instant expiresAt);
}
