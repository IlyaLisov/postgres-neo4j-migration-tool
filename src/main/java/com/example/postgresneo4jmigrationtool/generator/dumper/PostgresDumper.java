package com.example.postgresneo4jmigrationtool.generator.dumper;

import com.example.postgresneo4jmigrationtool.model.DumpResult;

import java.util.Map;

public interface PostgresDumper {

    DumpResult dump(String tableName, Map<String, String> columnsInfo);

}
