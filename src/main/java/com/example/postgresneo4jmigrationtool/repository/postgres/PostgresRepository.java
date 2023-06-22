package com.example.postgresneo4jmigrationtool.repository.postgres;

import java.util.Map;

public interface PostgresRepository {

    String getUsername();

    String getPassword();

    String getDatabaseName();

    String getSchemaName();

    Map<String, String> getColumnsInfo(String tableName);

}
