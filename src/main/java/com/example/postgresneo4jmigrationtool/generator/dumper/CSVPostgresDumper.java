package com.example.postgresneo4jmigrationtool.generator.dumper;

import com.example.postgresneo4jmigrationtool.model.MigrationData;
import com.example.postgresneo4jmigrationtool.model.exception.MigrationException;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${xml.delimiter}")
    private String delimiter;

    @Override
    public MigrationData dump(String tableName, Collection<String> columnsToDump) {
        MigrationData migrationData = new MigrationData();
        migrationData.add("dumpDirectory", dumpDirectory);
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
        addInputStream(migrationData, tableName);
        return migrationData;
    }

    @Override
    public MigrationData dumpWithForeignKeys(String tableName, String columnFrom, String columnTo) {
        MigrationData migrationData = new MigrationData();
        migrationData.add("dumpDirectory", dumpDirectory);
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
        addInputStream(migrationData, tableName);
        return migrationData;
    }

    @Override
    public MigrationData dumpInnerFields(String tableName, String columnFrom, String valueColumn) {
        MigrationData migrationData = new MigrationData();
        migrationData.add("dumpDirectory", dumpDirectory);
        File dumpScript = new File(dumpDirectory + "/" + dumpScriptFileName);
        createFile(dumpScript);
        String foreignColumnFrom = postgresRepository.getForeignColumnName(tableName, columnFrom);
        try (PrintWriter writer = new PrintWriter(dumpScript)) {
            writer.printf("psql -U %s -c \"COPY (SELECT %s as %s, %s FROM %s) TO STDOUT WITH CSV DELIMITER '%s' HEADER\" %s > %s.csv",
                    postgresRepository.getUsername(),
                    columnFrom,
                    foreignColumnFrom,
                    valueColumn,
                    tableName,
                    delimiter,
                    postgresRepository.getDatabaseName(),
                    tableName);
        } catch (IOException e) {
            throw new MigrationException("Exception during dumping: " + e.getMessage());
        }
        runScript(dumpScript);
        addInputStream(migrationData, tableName);
        return migrationData;
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

    private void addInputStream(MigrationData migrationData, String tableName) {
        try {
            InputStream inputStream = new FileInputStream(dumpDirectory + "/" + tableName + ".csv");
            migrationData.add("inputStream", inputStream);
        } catch (FileNotFoundException e) {
            throw new MigrationException("Migration script file was not found.");
        }
    }

}
