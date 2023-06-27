package com.example.postgresneo4jmigrationtool.generator.dumper;

import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.model.exception.MigrationException;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CSVPostgresDumper implements PostgresDumper {

    private final PostgresRepository postgresRepository;
    private final String dumpDirectory = "dump";
    private final String dumpScriptFileName = "dump_script.sh";
    private final String delimiter = ";";

    @Override
    public DumpResult dump(String tableName, Collection<String> columnsToDump) {
        DumpResult dumpResult = new DumpResult();
        dumpResult.add("dumpDirectory", dumpDirectory);
        dumpResult.add("delimiter", delimiter);
        File dumpScript = new File(dumpDirectory + "/" + dumpScriptFileName);
        createFile(dumpScript);
        String columns = String.join(",", columnsToDump);
        try (PrintWriter writer = new PrintWriter(dumpScript)) {
            writer.printf("psql -U %s -c \"COPY (SELECT %s FROM %s) TO STDOUT WITH CSV DELIMITER '%s' HEADER\" %s > %s.csv",
                    postgresRepository.getUsername(), columns, tableName, delimiter,
                    postgresRepository.getDatabaseName(), tableName);
        } catch (IOException e) {
            throw new MigrationException("Exception during dumping: " + e.getMessage());
        }
        runScript(dumpScript);
        addInputStream(dumpResult, tableName);
        return dumpResult;
    }

    @Override
    public DumpResult dumpWithForeignKeys(String tableName, String columnFrom, String columnTo) {
        DumpResult dumpResult = new DumpResult();
        dumpResult.add("dumpDirectory", dumpDirectory);
        dumpResult.add("delimiter", delimiter);
        File dumpScript = new File(dumpDirectory + "/" + dumpScriptFileName);
        createFile(dumpScript);
        String foreignColumnFrom = postgresRepository.getForeignColumnName(tableName, columnFrom);
        String foreignColumnTo = postgresRepository.getForeignColumnName(tableName, columnTo);
        try (PrintWriter writer = new PrintWriter(dumpScript)) {
            writer.printf("psql -U %s -c \"COPY (SELECT %s as %s, %s as %s FROM %s) TO STDOUT WITH CSV DELIMITER '%s' HEADER\" %s > %s.csv",
                    postgresRepository.getUsername(),
                    columnFrom,
                    foreignColumnFrom,
                    columnTo,
                    foreignColumnTo,
                    tableName,
                    delimiter,
                    postgresRepository.getDatabaseName(),
                    tableName);
        } catch (IOException e) {
            throw new MigrationException("Exception during dumping: " + e.getMessage());
        }
        runScript(dumpScript);
        addInputStream(dumpResult, tableName);
        return dumpResult;
    }

    @Override
    public DumpResult dumpInnerFieldTable(String tableName, String columnFrom, String columnTo) {
        DumpResult dumpResult = new DumpResult();
        dumpResult.add("dumpDirectory", dumpDirectory);
        dumpResult.add("delimiter", delimiter);
        File dumpScript = new File(dumpDirectory + "/" + dumpScriptFileName);
        createFile(dumpScript);
        String foreignColumnFrom = postgresRepository.getForeignColumnName(tableName, columnFrom);
        try (PrintWriter writer = new PrintWriter(dumpScript)) {
            writer.printf("psql -U %s -c \"COPY (SELECT %s as %s, %s FROM %s) TO STDOUT WITH CSV DELIMITER '%s' HEADER\" %s > %s.csv",
                    postgresRepository.getUsername(),
                    columnFrom,
                    foreignColumnFrom,
                    columnTo,
                    tableName,
                    delimiter,
                    postgresRepository.getDatabaseName(),
                    tableName);
        } catch (IOException e) {
            throw new MigrationException("Exception during dumping: " + e.getMessage());
        }
        runScript(dumpScript);
        addInputStream(dumpResult, tableName);
        return dumpResult;
    }

    private void createFile(File file) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) {
            throw new MigrationException("Exception during temp folder creation: " + e.getMessage());
        }
    }

    private void runScript(File dumpScript) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", dumpScript.getAbsolutePath());
            Map<String, String> env = processBuilder.environment();
            env.put("PGPASSWORD", postgresRepository.getPassword());
            processBuilder.directory(new File(dumpScript.getParent()));
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new MigrationException("Exception during dumping script running: " + e.getMessage());
        }
    }

    private void addInputStream(DumpResult dumpResult, String tableName) {
        try {
            InputStream inputStream = new FileInputStream(dumpDirectory + "/" + tableName + ".csv");
            dumpResult.add("inputStream", inputStream);
        } catch (FileNotFoundException e) {
            throw new MigrationException("Migration script file was not found.");
        }
    }

}
