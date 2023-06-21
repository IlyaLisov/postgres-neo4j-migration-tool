package com.example.postgresneo4jmigrationtool.command;

import com.example.postgresneo4jmigrationtool.generator.dumper.PostgresDumper;
import com.example.postgresneo4jmigrationtool.generator.uploader.Neo4jUploader;
import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.repository.neo4j.Neo4jRepository;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandLineUserUI implements UserUI {

    private final Scanner scanner;
    private final PostgresRepository postgresRepository;
    private final Neo4jRepository neo4jRepository;
    private final PostgresDumper postgresDumper;
    private final Neo4jUploader neo4jUploader;

    @Override
    public void run() {
        write("Postgres to Neo4j migration tool started.\n\n");
        write("You are connected to Postgres database: " + postgresRepository.getDatabaseName() + "\n");
        write("You are connected to Postgres schema: " + postgresRepository.getSchemaName() + "\n");
        write("You are connected to Neo4j database: " + neo4jRepository.getDatabaseName() + "\n");
        writeOptionMenu();
        String option = read();
        if (option.equals("1")) {
            handleShowTables();
        } else {
            write("Wrong option.\n");
        }
    }

    @Override
    public String read() {
        return scanner.next();
    }

    @Override
    public void write(String message) {
        System.out.print(message);
    }

    private void writeOptionMenu() {
        write("Choose next option: \n");
        write("1 - Show all Postgres tables\n");
    }

    private void handleShowTables() {
        List<String> tablesNames = postgresRepository.getTablesNames();
        write("\nHere are your tables: \n");
        for (int i = 0; i < tablesNames.size(); i++) {
            write((i + 1) + " - " + tablesNames.get(i) + "\n");
        }
        write("Choose table to get info about: \n");
        int option = Integer.parseInt(read());
        if (option > 0 && option <= tablesNames.size()) {
            handleShowColumns(tablesNames.get(option - 1));
        } else {
            write("Wrong option.\n");
        }
    }

    private void handleShowColumns(String tableName) {
        Map<String, String> columnNames = postgresRepository.getColumnsInfo(tableName);
        write("\nHere are your table " + tableName + ": \n");
        printColumns(columnNames);
        handleAvoidColumns(columnNames, tableName);
    }

    private void printColumns(Map<String, String> columnNames) {
        write(String.format("%20s", "Column name") + String.format("%20s", "Data type") + "\n");
        int counter = 1;
        for (String key : columnNames.keySet()) {
            write(String.format("%5o", counter++) + String.format("%20s", key) + String.format("%20s", columnNames.get(key)) + "\n");
        }
    }

    private void handleAvoidColumns(Map<String, String> columnNames, String tableName) {
        write("\nDo you want to avoid any columns from migrating?\n");
        write("Write column numbers divided by ',' to exclude them from migrating. Or write 'n' to skip this step.\n");
        String answer = read();
        if (!answer.equals("n")) {
            Set<Integer> ids = Arrays.stream(answer.split(","))
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());
            for (int id : ids) {
                if (id < 1 || id > columnNames.size()) {
                    throw new IllegalArgumentException("Column with id " + id + " is not in range.");
                }
            }
            List<String> keysToRemove = new ArrayList<>();
            for (int position : ids) {
                int index = 1;
                for (String key : columnNames.keySet()) {
                    if (index == position) {
                        keysToRemove.add(key);
                        break;
                    }
                    index++;
                }
            }
            for (String key : keysToRemove) {
                columnNames.remove(key);
            }
        }
        handleFormatTime(columnNames, tableName);
    }

    private void handleFormatTime(Map<String, String> columnNames, String tableName) {
        write("\nDo you need to format date fields?\n");
        write("Write format pattern. Or write 'n' to skip this step.\n");
        String answer = read();
        if (!answer.equals("n")) {
            write("Your date fields will be converted by this pattern: " + answer + "\n");
        }
        handleColumnsNames(columnNames, tableName, answer);
    }

    private void handleColumnsNames(Map<String, String> columnNames, String tableName, String datePattern) {
        write("\nHere are your table " + tableName + " for migration: \n");
        printColumns(columnNames);
        write("\nDo you want to rename any columns?\n");
        write("Write column numbers and new column names divided by '-' and ',' between pair of number and name to rename them before migrating. Or write 'n' to skip this step.\n");
        String answer = read();
        Map<String, String> keysToRename = new HashMap<>();
        if (!answer.equals("n")) {
            List<Map<Integer, String>> names = Arrays.stream(answer.split(","))
                    .map(s -> {
                        Map<Integer, String> map = new HashMap<>();
                        int index = s.indexOf('-');
                        String number = s.substring(0, index);
                        String name = s.substring(index + 1);
                        map.put(Integer.valueOf(number), name);
                        return map;
                    }).toList();
            Map<Integer, String> newNames = new HashMap<>();
            for (Map<Integer, String> map : names) {
                newNames.putAll(map);
            }
            for (int id : newNames.keySet()) {
                if (id < 1 || id > columnNames.size()) {
                    throw new IllegalArgumentException("Column with id " + id + " is not in range.");
                }
            }
            for (int position : newNames.keySet()) {
                int index = 1;
                for (String key : columnNames.keySet()) {
                    if (index == position) {
                        keysToRename.put(key, newNames.get(position));
                        break;
                    }
                    index++;
                }
            }
        }
        handleBeginMigration(columnNames, keysToRename, tableName, datePattern);
    }

    private void handleBeginMigration(Map<String, String> columnNames, Map<String, String> newNames, String tableName, String datePattern) {
        write("\nHere are your table " + tableName + " for migration: \n");
        printColumns(columnNames);
        write("\nAnd next columns will be renamed: \n");
        write(String.format("%20s", "Column name") + String.format("%20s", "New column name") + "\n");
        int counter = 1;
        for (String key : newNames.keySet()) {
            write(String.format("%5o", counter++) + String.format("%20s", key) + String.format("%20s", newNames.get(key)) + "\n");
        }
        handleSelectLabels(columnNames, newNames, tableName, datePattern);
    }

    public void handleSelectLabels(Map<String, String> columnNames, Map<String, String> newNames, String tableName, String datePattern) {
        write("\nDo you want to add labels to nodes from this table?\n");
        write("Write labels for these nodes divided by ','. Or write 'n' to skip this step.\n");
        String options = read();
        String[] labels = new String[0];
        if (!options.equals("n")) {
            labels = options.split(",");
        }
        write("\nNext labels will be added to these nodes: \n");
        for (String label : labels) {
            write("- " + label + "\n");
        }
        write("\nIf everything is OK and you want to start migration, write 'y', otherwise write 'n'.\n");
        String answer = read();
        switch (answer) {
            case "y" ->
                    handleMigration(columnNames, newNames, tableName, labels, datePattern);
            case "n" -> write("\nBye.");
        }
    }

    private void handleMigration(Map<String, String> columnNames, Map<String, String> newNames, String tableName, String[] labels, String datePattern) {
        write("\nStart dumping table " + tableName + "\n");
        DumpResult dumpResult = postgresDumper.dump(tableName, columnNames);
        write("Table " + tableName + " dumped to " + dumpResult.get("dumpDirectory"));
        write("\nStart uploading table " + tableName + "\n");
        UploadParams uploadParams = new UploadParams();
        uploadParams.add("newNames", newNames);
        uploadParams.add("delimeter", dumpResult.get("delimeter"));
        uploadParams.add("labels", labels);
        uploadParams.add("datePattern", datePattern);
        UploadResult uploadResult = neo4jUploader.upload((InputStream) dumpResult.get("inputStream"), uploadParams);
        write("Table " + tableName + " successfully uploaded to Neo4j.\n");
        write("Loaded " + uploadResult.get("nodeCounter") + " nodes.");
    }

}
