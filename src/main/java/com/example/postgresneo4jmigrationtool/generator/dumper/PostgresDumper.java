package com.example.postgresneo4jmigrationtool.generator.dumper;

import java.util.Map;

public interface PostgresDumper {

    void dump(String tableName, Map<String, String> params);

}
