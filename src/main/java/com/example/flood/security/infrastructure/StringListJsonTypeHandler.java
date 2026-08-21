package com.example.flood.security.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override public void setNonNullParameter(PreparedStatement ps, int i, List<String> value, JdbcType type)
        throws SQLException { ps.setString(i, write(value)); }
    @Override public List<String> getNullableResult(ResultSet rs, String column) throws SQLException {
        return read(rs.getString(column));
    }
    @Override public List<String> getNullableResult(ResultSet rs, int column) throws SQLException {
        return read(rs.getString(column));
    }
    @Override public List<String> getNullableResult(CallableStatement cs, int column) throws SQLException {
        return read(cs.getString(column));
    }
    private static List<String> read(String value) throws SQLException {
        if (value == null) return List.of();
        try { return MAPPER.readValue(value, TYPE); }
        catch (Exception exception) { throw new SQLException("Invalid string-list JSON", exception); }
    }
    private static String write(List<String> value) throws SQLException {
        try { return MAPPER.writeValueAsString(value); }
        catch (Exception exception) { throw new SQLException("Cannot write string-list JSON", exception); }
    }
}
