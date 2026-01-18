package com.campus.wall.config.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class JsonbLongListTypeHandler extends BaseTypeHandler<List<Long>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Long> parameter, JdbcType jdbcType)
        throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(json);
            ps.setObject(i, pgObject);
        } catch (Exception e) {
            throw new SQLException("Failed to convert List<Long> to jsonb", e);
        }
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readJson(rs.getObject(columnName));
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readJson(rs.getObject(columnIndex));
    }

    @Override
    public List<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readJson(cs.getObject(columnIndex));
    }

    private List<Long> readJson(Object value) throws SQLException {
        if (value == null) {
            return Collections.emptyList();
        }
        try {
            String json;
            if (value instanceof PGobject pg) {
                json = pg.getValue();
            } else {
                json = value.toString();
            }
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return OBJECT_MAPPER.readValue(json, LONG_LIST);
        } catch (Exception e) {
            throw new SQLException("Failed to parse jsonb to List<Long>", e);
        }
    }
}
