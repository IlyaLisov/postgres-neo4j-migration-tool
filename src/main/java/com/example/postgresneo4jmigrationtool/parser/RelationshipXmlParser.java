package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.generator.dumper.PostgresDumper;
import com.example.postgresneo4jmigrationtool.generator.uploader.Neo4jUploader;
import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import com.jcabi.xml.XML;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

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
            String sourceColumn = new TextXpath(table).getInnerValue("configuration", "sourceColumn");
            String targetColumn = new TextXpath(table).getInnerValue("configuration", "targetColumn");
            String sourceLabel = new TextXpath(table).getInnerValue("configuration", "sourceLabel");
            String targetLabel = new TextXpath(table).getInnerValue("configuration", "targetLabel");
            String type = new TextXpath(table).getValue("type");
            if (type.isEmpty()) {
                throw new InvalidConfigurationException("Relationship must have type.");
            }
            String sourceColumnType = postgresRepository.getColumnType(tableName, sourceColumn);
            String targetColumnType = postgresRepository.getColumnType(tableName, sourceColumn);
            DumpResult dumpResult = dumper.dumpWithForeignKeys(tableName, sourceColumn, targetColumn);
            UploadParams uploadParams = new UploadParams();
            uploadParams.add("type", type);
            uploadParams.add("sourceLabel", sourceLabel);
            uploadParams.add("targetLabel", targetLabel);
            uploadParams.add("sourceColumnType", sourceColumnType);
            uploadParams.add("targetColumnType", targetColumnType);
            UploadResult uploadResult = uploader.createRelationship((InputStream) dumpResult.get("inputStream"), uploadParams);
            System.out.println("Table " + tableName + " successfully uploaded to Neo4j.");
            System.out.println("Created " + uploadResult.get("relationshipCounter") + " relationships.\n");
        }
    }

}
