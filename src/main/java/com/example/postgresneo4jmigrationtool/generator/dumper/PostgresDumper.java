package com.example.postgresneo4jmigrationtool.generator.dumper;

import com.example.postgresneo4jmigrationtool.model.DumpResult;

import java.util.Collection;

public interface PostgresDumper {

    DumpResult dump(String tableName, Collection<String> columnsToDump);

    DumpResult dumpWithForeignKeys(String tableName, String columnFrom, String columnTo);

}
