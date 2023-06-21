package com.example.postgresneo4jmigrationtool.repository.postgres;

import java.util.List;

public interface PostgresRepository {

    List<String> getColumnNames();

}
