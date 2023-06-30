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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service(value = "relationshipXmlParser")
@Data
public class RelationshipXmlParser implements Parser {

    private final PostgresRepository postgresRepository;
    private final PostgresDumper dumper;
    private final Neo4jUploader uploader;

    private List<XML> tables;

    @Override
    public void parse() {
        for (XML table : tables) {
            String tableName = new TextXpath(table).getAttribute("name");
            List<XML> configurationTag = table.nodes("configuration");
            String sourceColumn = new TextXpath(table).getInnerValue("configuration", "sourceColumn");
            String targetColumn = new TextXpath(table).getInnerValue("configuration", "targetColumn");
            String sourceLabel = new TextXpath(table).getInnerValue("configuration", "sourceLabel");
            String targetLabel = new TextXpath(table).getInnerValue("configuration", "targetLabel");
            Map<String, List<String>> followRows = new LinkedHashMap<>();
            Map<String, List<String>> skipRows = new LinkedHashMap<>();
            String type = new TextXpath(table).getInnerValue("type");
            if (type.isEmpty()) {
                throw new InvalidConfigurationException("Relationship must have type.");
            }
            if (!configurationTag.isEmpty()) {
                XML configuration = configurationTag.get(0);
                followRows = new TextXpath(configuration).getPreferredColumns("follow");
                skipRows = new TextXpath(configuration).getPreferredColumns("skip");
            }
            String sourceColumnType = postgresRepository.getColumnType(tableName, sourceColumn);
            String targetColumnType = postgresRepository.getColumnType(tableName, sourceColumn);
            MigrationData dumpParams = new MigrationData();
            dumpParams.add("followRows", followRows);
            dumpParams.add("skipRows", skipRows);
            MigrationData migrationData = dumper.dumpWithForeignKeys(tableName, sourceColumn, targetColumn, dumpParams);
            MigrationData uploadParams = new MigrationData();
            uploadParams.add("type", type);
            uploadParams.add("sourceLabel", sourceLabel);
            uploadParams.add("targetLabel", targetLabel);
            uploadParams.add("sourceColumnType", sourceColumnType);
            uploadParams.add("targetColumnType", targetColumnType);
            MigrationData uploadResult = uploader.createRelationship((InputStream) migrationData.get("inputStream"), uploadParams);
            System.out.println("Table " + tableName + " successfully uploaded to Neo4j.");
            System.out.println("Created " + uploadResult.get("relationshipCounter") + " relationships.\n");
        }
    }

}
