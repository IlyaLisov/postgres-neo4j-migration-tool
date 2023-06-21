package com.example.postgresneo4jmigrationtool.repository.postgres;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostgresRepositoryImpl implements PostgresRepository {

    @Override
    public List<String> getColumnNames() {
        return null;
    }

}
