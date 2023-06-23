package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.generator.dumper.PostgresDumper;
import com.example.postgresneo4jmigrationtool.generator.uploader.Neo4jUploader;
import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import com.jcabi.xml.XML;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class XmlParser implements Parser {

    private final PostgresRepository postgresRepository;
    private final PostgresDumper dumper;
    private final Neo4jUploader neo4jUploader;

    private final XML xml;

    @Override
    public void parse() {
        XML root = xml.nodes("migration").get(0);
        MigrationType type = MigrationType.valueOf(root.xpath("@type").get(0).toUpperCase());
        if (type == MigrationType.NODE) {
            parseNodeMigration(root);
        } else if (type == MigrationType.RELATIONSHIP) {
            parseRelationshipMigration(root);
        }
    }

    private void parseNodeMigration(XML root) {
        List<XML> tables = root.nodes("tables")
                .get(0)
                .nodes("table");
        for (XML table : tables) {
            String tableName = getTableName(table);
            List<XML> configurationTag = table.nodes("configuration");
            List<String> excludedColumns = new ArrayList<>();
            Map<String, String> renamedColumns = new HashMap<>();
            if (!configurationTag.isEmpty()) {
                XML configuration = configurationTag.get(0);
                excludedColumns = getExcludedColumns(configuration);
                renamedColumns = getRenamedColumns(configuration);
            }
            List<XML> labelsTag = table.nodes("labels");
            List<String> labels = new ArrayList<>();
            if (!labelsTag.isEmpty()) {
                labels = getLabels(labelsTag.get(0));
            }
            Map<String, String> tablesToDump = getTables(tableName, excludedColumns);
            DumpResult dumpResult = dumper.dump(tableName, tablesToDump.keySet());
            UploadParams uploadParams = new UploadParams();
            uploadParams.add("newNames", renamedColumns);
            uploadParams.add("delimiter", dumpResult.get("delimiter"));
            uploadParams.add("labels", labels);
            UploadResult uploadResult = neo4jUploader.createNode((InputStream) dumpResult.get("inputStream"), uploadParams);
            System.out.println("Table " + tableName + " successfully uploaded to Neo4j.");
            System.out.println("Created " + uploadResult.get("nodeCounter") + " nodes.\n");
        }
    }

    private void parseRelationshipMigration(XML root) {
        List<XML> tables = root.nodes("tables").get(0).nodes("table");
        for (XML table : tables) {
            String tableName = getTableName(table);
            String columnFrom = getConfigurationTagValue(table, "columnFrom");
            String columnTo = getConfigurationTagValue(table, "columnTo");
            String labelFrom = getConfigurationTagValue(table, "labelFrom");
            String labelTo = getConfigurationTagValue(table, "labelTo");
            String type = getType(table);
            DumpResult dumpResult = dumper.dumpWithForeignKeys(tableName, columnFrom, columnTo);
            UploadParams uploadParams = new UploadParams();
            uploadParams.add("delimiter", dumpResult.get("delimiter"));
            uploadParams.add("type", type);
            uploadParams.add("labelFrom", labelFrom);
            uploadParams.add("labelTo", labelTo);
            UploadResult uploadResult = neo4jUploader.createRelationship((InputStream) dumpResult.get("inputStream"), uploadParams);
            System.out.println("Table " + tableName + " successfully uploaded to Neo4j.");
            System.out.println("Created " + uploadResult.get("relationshipCounter") + " relationships.\n");
        }
    }

    private String getTableName(XML table) {
        return table.xpath("@name").get(0);
    }

    private List<String> getExcludedColumns(XML configuration) {
        XML excludeColumns = configuration.nodes("excludedColumns").get(0);
        return excludeColumns.nodes("column").stream()
                .map(c -> c.xpath("text()").get(0))
                .toList();
    }

    private Map<String, String> getRenamedColumns(XML configuration) {
        XML renameColumns = configuration.nodes("renamedColumns").get(0);
        List<XML> columns = renameColumns.nodes("columns");
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
        Map<String, String> renamedColumns = new HashMap<>();
        for (int i = 0; i < previousNames.size(); i++) {
            renamedColumns.put(previousNames.get(i), newNames.get(i));
        }
        return renamedColumns;
    }

    private Map<String, String> getTables(String tableName, List<String> excludedColumns) {
        Map<String, String> tables = postgresRepository.getColumnsInfo(tableName);
        for (String name : excludedColumns) {
            tables.remove(name);
        }
        return tables;
    }

    private List<String> getLabels(XML labels) {
        return labels.nodes("label").stream()
                .map(l -> l.xpath("text()").get(0))
                .toList();
    }

    private String getType(XML table) {
        return table.nodes("type")
                .get(0)
                .xpath("text()")
                .get(0);
    }

    private String getConfigurationTagValue(XML table, String tag) {
        return table.nodes("configuration")
                .get(0)
                .nodes(tag)
                .get(0)
                .xpath("text()")
                .get(0);
    }

}