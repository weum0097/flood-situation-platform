package com.example.flood.security.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class StringSetJsonTypeHandler extends BaseTypeHandler<Set<String>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Set<String>> TYPE = new TypeReference<>() {};

    @Override public void setNonNullParameter(PreparedStatement ps, int i, Set<String> value, JdbcType type)
        throws SQLException { ps.setString(i, write(value)); }
    @Override public Set<String> getNullableResult(ResultSet rs, String column) throws SQLException {
        return read(rs.getString(column));
    }
    @Override public Set<String> getNullableResult(ResultSet rs, int column) throws SQLException {
        return read(rs.getString(column));
    }
    @Override public Set<String> getNullableResult(CallableStatement cs, int column) throws SQLException {
        return read(cs.getString(column));
    }
    private static Set<String> read(String value) throws SQLException {
        if (value == null) return Set.of();
        try { return MAPPER.readValue(value, TYPE); }
        catch (Exception exception) { throw new SQLException("Invalid string-set JSON", exception); }
    }
    private static String write(Set<String> value) throws SQLException {
        try { return MAPPER.writeValueAsString(value); }
        catch (Exception exception) { throw new SQLException("Cannot write string-set JSON", exception); }
    }
}
