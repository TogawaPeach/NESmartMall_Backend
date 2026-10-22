package com.nemall.product.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** 普通商品和秒杀商品共用的图片 JSON 转换。 */
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    public static List<String> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<String> images = JSON.readValue(json, TYPE);
        return images == null ? List.of() : images;
    }

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int index, List<String> value,
            JdbcType jdbcType) throws SQLException {
        ps.setString(index, JSON.writeValueAsString(value));
    }

    @Override
    public List<String> getNullableResult(
            ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(
            ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(
            CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }
}
