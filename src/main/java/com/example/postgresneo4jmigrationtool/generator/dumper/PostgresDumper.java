package com.example.postgresneo4jmigrationtool.generator.dumper;

import com.example.postgresneo4jmigrationtool.model.MigrationData;

import java.util.Collection;

public interface PostgresDumper {

    MigrationData dump(String tableName, Collection<String> columnsToDump);

    MigrationData dumpWithForeignKeys(String tableName, String columnFrom, String columnTo);

    MigrationData dumpInnerFields(String tableName, String columnFrom, String valueColumn);

}
