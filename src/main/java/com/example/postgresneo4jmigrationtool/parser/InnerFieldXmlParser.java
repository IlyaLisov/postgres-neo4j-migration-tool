package com.example.postgresneo4jmigrationtool.parser;

import com.example.postgresneo4jmigrationtool.generator.dumper.PostgresDumper;
import com.example.postgresneo4jmigrationtool.generator.uploader.Neo4jUploader;
import com.example.postgresneo4jmigrationtool.model.DumpResult;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.repository.postgres.PostgresRepository;
import com.jcabi.xml.XML;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service(value = "innerFieldXmlParser")
@Data
public class InnerFieldXmlParser implements Parser {

    private final PostgresRepository postgresRepository;
    private final PostgresDumper dumper;
    private final Neo4jUploader uploader;

    private List<XML> tables;

    @Override
    public void parse() {
        for (XML table : tables) {
            String tableName = new TextXpath(table).getAttribute("name");
            String sourceColumn = new TextXpath(table).getInnerValue("configuration", "sourceColumn");
            String valueColumn = new TextXpath(table).getInnerValue("configuration", "valueColumn");
            String sourceLabel = new TextXpath(table).getInnerValue("configuration", "sourceLabel");
            String unique = new TextXpath(table).getInnerValue("configuration", "unique");
            String fieldName = new TextXpath(table).getInnerValue("configuration", "fieldName");
            String sourceColumnType = postgresRepository.getColumnType(tableName, sourceColumn);
            String valueColumnType = postgresRepository.getColumnType(tableName, valueColumn);
            DumpResult dumpResult = dumper.dumpInnerFields(tableName, sourceColumn, valueColumn);
            UploadParams uploadParams = new UploadParams();
            uploadParams.add("sourceLabel", sourceLabel);
            uploadParams.add("sourceColumnType", sourceColumnType);
            uploadParams.add("valueColumnType", valueColumnType);
            uploadParams.add("unique", unique.equals("true"));
            uploadParams.add("fieldName", fieldName);
            UploadResult uploadResult = uploader.createInnerField((InputStream) dumpResult.get("inputStream"), uploadParams);
            System.out.println("Table " + tableName + " successfully uploaded to Neo4j.");
            System.out.println("Created " + uploadResult.get("objectCounter") + " objects.\n");
        }
    }

}
