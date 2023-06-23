package com.example.postgresneo4jmigrationtool.repository.postgres;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PostgresRepositoryImpl implements PostgresRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url}")
    private String datasourceURL;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        int beginIndex = datasourceURL.lastIndexOf('/') + 1;
        int endInder = datasourceURL.indexOf('?');
        return datasourceURL.substring(beginIndex, endInder);
    }

    @Override
    public String getSchemaName() {
        int beginIndex = datasourceURL.lastIndexOf("=") + 1;
        return datasourceURL.substring(beginIndex);
    }

    @Override
    public Map<String, String> getColumnsInfo(String tableName) {
        String query = """
                SELECT column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = '%s'
                AND table_name   = '%s';
                """;
        String formattedQuery = String.format(query, getSchemaName(), tableName);
        List<Map<String, String>> resultList = jdbcTemplate.query(formattedQuery, (rs, rowNum) -> {
            String columnName = rs.getString("column_name");
            String columnType = rs.getString("data_type");
            Map<String, String> map = new HashMap<>();
            map.put(columnName, columnType);
            return map;
        });
        Map<String, String> columnsInfo = new HashMap<>();
        for (Map<String, String> map : resultList) {
            columnsInfo.putAll(map);
        }
        return columnsInfo;
    }

    @Override
    public String getForeignColumnName(String tableName, String columnName) {
        String query = """
                SELECT ccu.table_name  AS foreign_table_name,
                       ccu.column_name AS foreign_column_name
                FROM information_schema.table_constraints AS tc
                         JOIN information_schema.key_column_usage AS kcu
                              ON tc.constraint_name = kcu.constraint_name
                                  AND tc.table_schema = kcu.table_schema
                         JOIN information_schema.constraint_column_usage AS ccu
                              ON ccu.constraint_name = tc.constraint_name
                                  AND ccu.table_schema = tc.table_schema
                WHERE tc.constraint_type = 'FOREIGN KEY'
                  AND tc.table_name = '%s'
                  AND kcu.column_name = '%s';
                """;
        String formattedQuery = String.format(query, tableName, columnName);
        return jdbcTemplate.query(formattedQuery, (rs, rowNum) ->
                        rs.getString("foreign_column_name"))
                .get(0);
    }
}
