package com.nemall.product;

import com.nemall.product.controller.SeckillController;
import com.nemall.product.exception.ProductBusinessException;
import com.nemall.product.exception.ProductExceptionHandler;
import com.nemall.product.handler.StringListJsonTypeHandler;
import com.nemall.product.service.SeckillService;
import com.nemall.product.vo.ProductDetailVO;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeckillProductDetailTests {

    @Test
    void detailRouteReturnsCompletePayloadWithLocalActivityTimes() throws Exception {
        SeckillService service = mock(SeckillService.class);
        ProductDetailVO detail = new ProductDetailVO(
                "25", "秒杀商品", "型号", "cover.jpg", 100L,
                0L, 0L, List.of("detail.jpg"), true,
                LocalDateTime.parse("2026-10-22T16:00:00"),
                LocalDateTime.parse("2026-10-30T16:00:00"));
        when(service.getSeckillProductDetail(25L)).thenReturn(detail);

        MockMvcBuilders.standaloneSetup(new SeckillController(service)).build()
                .perform(get("/api/seckill/products/25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value("25"))
                .andExpect(jsonPath("$.data.name").value("秒杀商品"))
                .andExpect(jsonPath("$.data.model").value("型号"))
                .andExpect(jsonPath("$.data.coverUrl").value("cover.jpg"))
                .andExpect(jsonPath("$.data.price").value(100))
                .andExpect(jsonPath("$.data.sales").value(0))
                .andExpect(jsonPath("$.data.stock").value(0))
                .andExpect(jsonPath("$.data.detailImages[0]").value("detail.jpg"))
                .andExpect(jsonPath("$.data.isSeckill").value(true))
                .andExpect(jsonPath("$.data.seckillStartTime").value("2026-10-22T16:00:00"))
                .andExpect(jsonPath("$.data.seckillEndTime").value("2026-10-30T16:00:00"));
    }

    @Test
    void missingProductReturnsUnified404Response() throws Exception {
        SeckillService service = mock(SeckillService.class);
        when(service.getSeckillProductDetail(25L))
                .thenThrow(new ProductBusinessException(404, "秒杀商品不存在"));

        MockMvcBuilders.standaloneSetup(new SeckillController(service))
                .setControllerAdvice(new ProductExceptionHandler()).build()
                .perform(get("/api/seckill/products/25"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void mybatisMapsDatabaseColumnsDirectlyToCompleteDetail() throws Exception {
        Configuration config = new Configuration();
        String resource = "mapper/SeckillMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }

        MappedStatement mapping = config.getMappedStatement(
                "com.nemall.product.mapper.SeckillMapper.selectSeckillProductDetailById");
        var sql = mapping.getBoundSql(Map.of("id", 25L));
        assertThat(sql.getSql())
                .contains("WHERE id = ?", "AND is_seckill = 1",
                        "AND status = 1", "AND is_deleted = 0")
                .doesNotContain("NOW(", "CURRENT_TIMESTAMP", "BETWEEN");

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 25L);
        row.put("name", "秒杀商品");
        row.put("model", "型号");
        row.put("cover_image_url", "cover.jpg");
        row.put("price", 100L);
        row.put("sales_count", 7L);
        row.put("stock", 0L);
        row.put("detail_image_urls", "[\"detail1.jpg\",\"detail2.jpg\"]");
        row.put("is_seckill", true);
        row.put("seckill_start_time", LocalDateTime.parse("2026-10-22T16:00:00"));
        row.put("seckill_end_time", LocalDateTime.parse("2026-10-30T16:00:00"));

        DefaultResultSetHandler handler = new DefaultResultSetHandler(
                null, mapping, null, null, sql, RowBounds.DEFAULT);
        List<Object> mapped = handler.handleResultSets(statement(row));

        assertThat(mapped).hasSize(1);
        ProductDetailVO detail = (ProductDetailVO) mapped.getFirst();
        assertThat(detail.getId()).isEqualTo("25");
        assertThat(detail.getName()).isEqualTo("秒杀商品");
        assertThat(detail.getModel()).isEqualTo("型号");
        assertThat(detail.getCoverUrl()).isEqualTo("cover.jpg");
        assertThat(detail.getPrice()).isEqualTo(100L);
        assertThat(detail.getSales()).isEqualTo(7L);
        assertThat(detail.getStock()).isZero();
        assertThat(detail.getDetailImages()).containsExactly("detail1.jpg", "detail2.jpg");
        assertThat(detail.getIsSeckill()).isTrue();
        assertThat(detail.getSeckillStartTime()).isEqualTo("2026-10-22T16:00:00");
        assertThat(detail.getSeckillEndTime()).isEqualTo("2026-10-30T16:00:00");
    }

    @Test
    void sharedImageParserHandlesEmptyValuesWithoutHidingMalformedJson() {
        assertThat(StringListJsonTypeHandler.parse(null)).isEmpty();
        assertThat(StringListJsonTypeHandler.parse(" ")).isEmpty();
        assertThat(StringListJsonTypeHandler.parse("null")).isEmpty();
        assertThat(StringListJsonTypeHandler.parse("[]")).isEmpty();
        assertThat(StringListJsonTypeHandler.parse("[\"detail.jpg\"]"))
                .containsExactly("detail.jpg");
        assertThatThrownBy(() -> StringListJsonTypeHandler.parse("invalid"))
                .isInstanceOf(RuntimeException.class);
    }

    /** Only JDBC I/O is stubbed; the XML, result mapping and TypeHandler run normally. */
    private Statement statement(Map<String, Object> row) throws Exception {
        List<String> columns = List.copyOf(row.keySet());
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(columns.size());
        when(metadata.getColumnLabel(anyInt()))
                .thenAnswer(call -> columns.get(call.<Integer>getArgument(0) - 1));
        when(metadata.getColumnName(anyInt()))
                .thenAnswer(call -> columns.get(call.<Integer>getArgument(0) - 1));
        when(metadata.getColumnType(anyInt())).thenReturn(Types.VARCHAR);
        when(metadata.getColumnClassName(anyInt())).thenReturn(String.class.getName());

        ResultSet rows = mock(ResultSet.class);
        when(rows.getMetaData()).thenReturn(metadata);
        when(rows.next()).thenReturn(true, false);
        when(rows.getString(anyString())).thenAnswer(call -> {
            Object value = row.get(call.<String>getArgument(0));
            return value == null ? null : value.toString();
        });
        when(rows.getLong(anyString()))
                .thenAnswer(call -> ((Number) row.get(call.<String>getArgument(0))).longValue());
        when(rows.getBoolean(anyString()))
                .thenAnswer(call -> row.get(call.<String>getArgument(0)));
        when(rows.getObject(anyString(), eq(LocalDateTime.class)))
                .thenAnswer(call -> row.get(call.<String>getArgument(0)));

        DatabaseMetaData database = mock(DatabaseMetaData.class);
        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(database);
        Statement statement = mock(Statement.class);
        when(statement.getResultSet()).thenReturn(rows);
        when(statement.getConnection()).thenReturn(connection);
        when(statement.getUpdateCount()).thenReturn(-1);
        return statement;
    }
}
