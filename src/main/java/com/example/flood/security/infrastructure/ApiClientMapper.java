package com.example.flood.security.infrastructure;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ApiClientMapper {
    @Update("""
        INSERT INTO api_client (client_code, client_name, status, rate_limit_per_minute)
        VALUES (#{code}, #{code}, 'ACTIVE', 60)
        ON DUPLICATE KEY UPDATE status = 'ACTIVE'
        """)
    int upsert(@Param("code") String code);

    @Select("SELECT id FROM api_client WHERE client_code = #{code} LIMIT 1")
    Optional<Long> findIdByCode(@Param("code") String code);
}
