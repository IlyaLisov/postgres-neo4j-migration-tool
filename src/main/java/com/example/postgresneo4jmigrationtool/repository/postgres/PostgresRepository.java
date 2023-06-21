package com.example.postgresneo4jmigrationtool.repository.postgres;

import java.util.List;
import java.util.Map;

public interface PostgresRepository {

    String getUsername();

    String getPassword();

    String getDatabaseName();

    String getSchemaName();

    List<String> getTablesNames();

    Map<String, String> getColumnsInfo(String tableName);

}
