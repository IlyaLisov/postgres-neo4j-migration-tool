package com.example.postgresneo4jmigrationtool.generator.dumper;

import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CSVPostgresDumper implements PostgresDumper {

    private final PostgresRepository postgresRepository;
    private final String dumpDirectory = "dump";
    private final String dumpScriptFileName = "dump_script.sh";
    private final String delimeter = ";";

    @Override
    public DumpResult dump(String tableName, Map<String, String> columnsInfo) {
        DumpResult dumpResult = new DumpResult();
        dumpResult.add("dumpDirectory", dumpDirectory);
        dumpResult.add("delimeter", delimeter);

        File dumpScript = new File(dumpDirectory + "/" + dumpScriptFileName);
        createFile(dumpScript);
        String columns = String.join(",", columnsInfo.keySet());
        try (PrintWriter writer = new PrintWriter(dumpScript)) {
            writer.printf("psql -U %s -c \"COPY (SELECT %s FROM %s) TO STDOUT WITH CSV DELIMITER '%s' HEADER\" %s > %s.csv",
                    postgresRepository.getUsername(), columns, tableName, delimeter,
                    postgresRepository.getDatabaseName(), tableName);
        } catch (Exception e) {
            throw new IllegalStateException("Exception during dumping: " + e.getMessage());
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", dumpScript.getAbsolutePath());
            Map<String, String> env = processBuilder.environment();
            env.put("PGPASSWORD", postgresRepository.getPassword());
            processBuilder.directory(new File(dumpScript.getParent()));
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new IllegalStateException("Exception during dumping: " + e.getMessage());
        }

        try {
            InputStream inputStream = new FileInputStream(dumpDirectory + "/" + tableName + ".csv");
            dumpResult.add("inputStream", inputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return dumpResult;
    }

    private void createFile(File file) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
    }

}
