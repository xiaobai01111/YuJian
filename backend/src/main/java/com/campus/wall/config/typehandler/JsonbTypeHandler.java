package com.campus.wall.config.typehandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JsonbTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
        throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(json);
            ps.setObject(i, pgObject);
        } catch (Exception e) {
            throw new SQLException("Failed to convert parameter to jsonb", e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readJson(rs.getObject(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readJson(rs.getObject(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readJson(cs.getObject(columnIndex));
    }

    private Object readJson(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        try {
            String json;
            if (value instanceof PGobject pg) {
                json = pg.getValue();
            } else {
                json = value.toString();
            }
            if (json == null || json.isBlank()) {
                return null;
            }
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return node;
        } catch (Exception e) {
            throw new SQLException("Failed to parse jsonb value", e);
        }
    }
}
