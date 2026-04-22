package tw.com.stockplatform.infrastructure.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * MyBatis TypeHandler：List<String> ↔ PostgreSQL text[]（Array）。
 * <p>
 * 用於 IN / ANY 陣列查詢，避免 N+1 問題。
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.ARRAY)
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
        throws SQLException {
        Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
    }
}
