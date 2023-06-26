package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.generator.dumper.PostgresDumper;
import com.example.postgresneo4jmigrationtool.generator.uploader.Neo4jUploader;
import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import com.jcabi.xml.XML;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
        try {
            List<XML> nodeMigration = root.nodes("node");
            List<XML> relationshipMigration = root.nodes("relationship");
            if (!nodeMigration.isEmpty()) {
                List<XML> tables = getXMLTables(nodeMigration.get(0));
                parseNodeMigration(tables);
            }
            if (!relationshipMigration.isEmpty()) {
                List<XML> tables = getXMLTables(relationshipMigration.get(0));
                parseRelationshipMigration(tables);
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigurationException("Unsupported migration type. Supported are: " + Arrays.stream(MigrationType.values()).map(s -> s.name().toLowerCase()).toList());
        }
    }

    private List<XML> getXMLTables(XML root) {
        List<XML> tablesTag = root.nodes("tables");
        if (tablesTag.isEmpty()) {
            throw new InvalidConfigurationException("You must provide <tables> tag.");
        }
        List<XML> tables = tablesTag.get(0)
                .nodes("table");
        if (tables.isEmpty()) {
            throw new InvalidConfigurationException("You must provide at least one <table> tag.");
        }
        return tables;
    }

    private void parseNodeMigration(List<XML> tables) {
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
            List<String> labels = getLabels(table);
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

    private void parseRelationshipMigration(List<XML> tables) {
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
        Node name = table.node().getAttributes().getNamedItem("name");
        if (name == null) {
            throw new InvalidConfigurationException("Table must contain name attribute.");
        }
        return name.getNodeValue();
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
            Map<String, String> renamedColumnsMap = new HashMap<>();
            for (int i = 0; i < previousNames.size(); i++) {
                renamedColumnsMap.put(previousNames.get(i), newNames.get(i));
            }
            return renamedColumnsMap;
        }
        return new HashMap<>();
    }

    private Map<String, String> getTables(String tableName, List<String> excludedColumns) {
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

    private String getType(XML table) {
        List<XML> type = table.nodes("type");
        if (type.isEmpty()) {
            throw new InvalidConfigurationException("Relationship must have type.");
        }
        return type.get(0)
                .xpath("text()")
                .get(0);
    }

    private String getConfigurationTagValue(XML table, String tag) {
        List<XML> tags = table.nodes("configuration")
                .get(0)
                .nodes(tag);
        if (!tags.isEmpty()) {
            return tags
                    .get(0)
                    .xpath("text()")
                    .get(0);
        } else {
            return "";
        }
    }

}
