package com.example.postgresneo4jmigrationtool.repository.postgres;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
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
        int endIndex = datasourceURL.indexOf('?');
        return datasourceURL.substring(beginIndex, endIndex);
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
        Map<String, String> columnsInfo = new LinkedHashMap<>();
        jdbcTemplate.query(formattedQuery, (rs, rowNum) -> {
            String columnName = rs.getString("column_name");
            String columnType = rs.getString("data_type");
            columnsInfo.put(columnName, columnType);
            return columnsInfo;
        });
        return columnsInfo;
    }

    @Override
    public String getColumnType(String tableName, String column) {
        String query = """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = '%s'
                AND table_name   = '%s'
                AND column_name = '%s';
                """;
        String formattedQuery = String.format(query, getSchemaName(), tableName, column);
        List<String> type = jdbcTemplate.query(formattedQuery, (rs, rowNum) ->
                rs.getString("data_type"));
        return type.get(0);
    }

    @Override
    public String getForeignColumnName(String tableName, String columnName) {
        String query = """
                SELECT ccu.column_name AS column_name
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
        List<String> rows = jdbcTemplate.query(formattedQuery, (rs, rowNum) ->
                rs.getString("column_name"));
        if (rows.isEmpty()) {
            return columnName;
        } else {
            return rows.get(0);
        }
    }

}
