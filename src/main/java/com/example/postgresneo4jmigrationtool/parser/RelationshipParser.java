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
import org.w3c.dom.Node;

import java.io.InputStream;
import java.util.List;

@Service(value = "relationshipParser")
@Data
public class RelationshipParser implements Parser {

    private final PostgresRepository postgresRepository;
    private final PostgresDumper dumper;
    private final Neo4jUploader uploader;

    private List<XML> tables;

    @Override
    public void parse() {
        for (XML table : tables) {
            String tableName = getTableName(table);
            String columnFrom = getConfigurationTagValue(table, "columnFrom");
            String columnTo = getConfigurationTagValue(table, "columnTo");
            String labelFrom = getConfigurationTagValue(table, "labelFrom");
            String labelTo = getConfigurationTagValue(table, "labelTo");
            String type = getType(table);
            String columnFromType = postgresRepository.getColumnType(tableName, columnFrom);
            String columnToType = postgresRepository.getColumnType(tableName, columnFrom);
            DumpResult dumpResult = dumper.dumpWithForeignKeys(tableName, columnFrom, columnTo);
            UploadParams uploadParams = new UploadParams();
            uploadParams.add("delimiter", dumpResult.get("delimiter"));
            uploadParams.add("type", type);
            uploadParams.add("labelFrom", labelFrom);
            uploadParams.add("labelTo", labelTo);
            uploadParams.add("columnFromType", columnFromType);
            uploadParams.add("columnToType", columnToType);
            UploadResult uploadResult = uploader.createRelationship((InputStream) dumpResult.get("inputStream"), uploadParams);
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

    private String getType(XML table) {
        List<XML> type = table.nodes("type");
        if (type.isEmpty()) {
            throw new InvalidConfigurationException("Relationship must have type.");
        }
        return type.get(0)
                .xpath("text()")
                .get(0);
    }

}
