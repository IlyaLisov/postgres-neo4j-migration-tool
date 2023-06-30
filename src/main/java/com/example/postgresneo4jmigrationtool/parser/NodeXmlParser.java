package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.generator.dumper.PostgresDumper;
import com.example.postgresneo4jmigrationtool.generator.uploader.Neo4jUploader;
import com.example.postgresneo4jmigrationtool.model.MigrationData;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import com.jcabi.xml.XML;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service(value = "nodeXmlParser")
@Data
public class NodeXmlParser implements Parser {

    private final PostgresRepository postgresRepository;
    private final PostgresDumper dumper;
    private final Neo4jUploader uploader;

    private List<XML> tables;

    @Override
    public void parse() {
        for (XML table : tables) {
            String tableName = new TextXpath(table).getAttribute("name");
            List<XML> configurationTag = table.nodes("configuration");
            List<String> excludedColumns = new ArrayList<>();
            Map<String, String> renamedColumns = new LinkedHashMap<>();
            Map<String, List<String>> followRows = new LinkedHashMap<>();
            Map<String, List<String>> skipRows = new LinkedHashMap<>();
            String timeFormat = "";
            if (!configurationTag.isEmpty()) {
                XML configuration = configurationTag.get(0);
                excludedColumns = getExcludedColumns(configuration);
                renamedColumns = getRenamedColumns(configuration);
                followRows = new TextXpath(configuration).getPreferredColumns("follow");
                skipRows = new TextXpath(configuration).getPreferredColumns("skip");
                timeFormat = new TextXpath(table).getInnerValue("configuration", "timeFormat");
            }
            List<String> labels = getLabels(table);
            Map<String, String> tablesToDump = getColumns(tableName, excludedColumns);
            MigrationData migrationData = dumper.dump(tableName, tablesToDump.keySet());
            MigrationData uploadParams = new MigrationData();
            uploadParams.add("newNames", renamedColumns);
            uploadParams.add("labels", labels);
            uploadParams.add("types", tablesToDump.values());
            uploadParams.add("timeFormat", timeFormat);
            uploadParams.add("followRows", followRows);
            uploadParams.add("skipRows", skipRows);
            MigrationData uploadResult = uploader.createNode((InputStream) migrationData.get("inputStream"), uploadParams);
            System.out.println("Table " + tableName + " successfully uploaded to Neo4j.");
            System.out.println("Created " + uploadResult.get("nodeCounter") + " nodes.\n");
        }
    }

    private List<String> getExcludedColumns(XML configuration) {
        List<XML> excludedColumns = configuration.nodes("excludedColumns");
        if (!excludedColumns.isEmpty()) {
            return excludedColumns.get(0)
                    .nodes("column").stream()
                    .map(c -> c.xpath("text()").get(0))
                    .toList();
        }
        return new ArrayList<>();
    }

    private Map<String, String> getRenamedColumns(XML configuration) {
        List<XML> renamedColumns = configuration.nodes("renamedColumns");
        if (!renamedColumns.isEmpty()) {
            List<XML> columns = renamedColumns.get(0)
                    .nodes("columns");
            List<String> previousNames = columns.stream()
                    .map(c -> c.nodes("previousName")
                            .get(0)
                            .xpath("text()")
                            .get(0))
                    .toList();
            List<String> newNames = columns.stream()
                    .map(c -> c.nodes("newName")
                            .get(0)
                            .xpath("text()")
                            .get(0))
                    .toList();
            if (previousNames.size() != newNames.size()) {
                throw new InvalidConfigurationException("Amount of <previousName> and <newName> tags must be the same.");
            }
            Map<String, String> renamedColumnsMap = new LinkedHashMap<>();
            for (int i = 0; i < previousNames.size(); i++) {
                renamedColumnsMap.put(previousNames.get(i), newNames.get(i));
            }
            return renamedColumnsMap;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, String> getColumns(String tableName, List<String> excludedColumns) {
        Map<String, String> tables = postgresRepository.getColumnsInfo(tableName);
        for (String name : excludedColumns) {
            tables.remove(name);
        }
        return tables;
    }

    private List<String> getLabels(XML table) {
        List<XML> labels = table.nodes("labels");
        if (!labels.isEmpty()) {
            return labels
                    .get(0)
                    .nodes("label").stream()
                    .map(l -> l.xpath("text()").get(0))
                    .toList();
        }
        return new ArrayList<>();
    }

}
