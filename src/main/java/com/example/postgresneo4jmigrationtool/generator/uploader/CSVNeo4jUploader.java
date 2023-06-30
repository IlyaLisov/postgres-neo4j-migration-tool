package com.example.postgresneo4jmigrationtool.generator.uploader;

import com.example.postgresneo4jmigrationtool.model.InnerField;
import com.example.postgresneo4jmigrationtool.model.MigrationData;
import com.example.postgresneo4jmigrationtool.model.Node;
import com.example.postgresneo4jmigrationtool.model.Relationship;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidFieldException;
import com.example.postgresneo4jmigrationtool.model.exception.MigrationException;
import com.example.postgresneo4jmigrationtool.repository.neo4j.Neo4jRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class CSVNeo4jUploader implements Neo4jUploader {

    private final Neo4jRepository neo4jRepository;

    @Value("${xml.delimiter}")
    private String delimiter;

    @SneakyThrows
    @Override
    public MigrationData createNode(InputStream inputStream, MigrationData params) {
        MigrationData result = new MigrationData();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(delimiter);
            if (columnNames.length == 0) {
                throw new MigrationException("First row of dumped file must contain column names.");
            }
            List<String> labels = (List<String>) params.get("labels");
            Map<String, String> newNames = (Map<String, String>) params.get("newNames");
            Collection<String> types = (Collection<String>) params.get("types");
            String timeFormat = (String) params.get("timeFormat");
            for (String name : newNames.values()) {
                if (name.contains(" ")) {
                    throw new InvalidFieldException("Field name can not include spaces: " + name);
                }
            }
            for (int i = 0; i < columnNames.length; i++) {
                if (columnNames[i].contains(" ")) {
                    throw new InvalidFieldException("Field name can not include spaces: " + columnNames[i]);
                }
                if (newNames.containsKey(columnNames[i])) {
                    columnNames[i] = newNames.get(columnNames[i]);
                }
            }
            int nodeCounter = 0;
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] values = data.split(delimiter);
                while (values[values.length - 1].startsWith("\"") && !values[values.length - 1].endsWith("\"")) {
                    String line = scanner.nextLine();
                    String[] additionalData = line.split(delimiter);
                    if (additionalData.length == 1) {
                        values[values.length - 1] += "\n" + line;
                    } else {
                        values[values.length - 1] += additionalData[0];
                        int previousLength = values.length;
                        values = Arrays.copyOf(values, values.length + additionalData.length - 1);
                        System.arraycopy(additionalData, 1, values, previousLength - 1 + 1, additionalData.length - 1);
                    }
                }
                Node node = new Node(columnNames, values, types.toArray(new String[0]), labels.toArray(new String[0]));
                node.setTimeFormat(timeFormat);
                neo4jRepository.addNode(node);
                nodeCounter++;
            }
            result.add("nodeCounter", nodeCounter);
        }
        return result;
    }

    @Override
    public MigrationData createRelationship(InputStream inputStream, MigrationData params) {
        MigrationData result = new MigrationData();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(delimiter);
            String sourceColumnType = (String) params.get("sourceColumnType");
            String targetColumnType = (String) params.get("targetColumnType");
            if (columnNames.length == 0) {
                throw new MigrationException("First row of dumped file must contain column names.");
            }
            for (String name : columnNames) {
                if (name.contains(" ")) {
                    throw new InvalidFieldException("Field name can not include spaces: " + name);
                }
            }
            String type = (String) params.get("type");
            if (type == null || type.isEmpty()) {
                throw new InvalidConfigurationException("Relationship must have type.");
            }
            String sourceLabel = (String) params.get("sourceLabel");
            String targetLabel = (String) params.get("targetLabel");
            int relationshipCounter = 0;
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] values = data.split(delimiter);
                while (values[values.length - 1].startsWith("\"") && !values[values.length - 1].endsWith("\"")) {
                    String line = scanner.nextLine();
                    String[] additionalData = line.split(delimiter);
                    if (additionalData.length == 1) {
                        values[values.length - 1] += "\n" + line;
                    } else {
                        values[values.length - 1] += additionalData[0];
                        int previousLength = values.length;
                        values = Arrays.copyOf(values, values.length + additionalData.length - 1);
                        System.arraycopy(additionalData, 1, values, previousLength - 1 + 1, additionalData.length - 1);
                    }
                }
                Node source = new Node(columnNames[0], values[0], sourceColumnType, sourceLabel);
                Node target = new Node(columnNames[1], values[1], targetColumnType, targetLabel);
                Relationship relationship = new Relationship(source, target);
                neo4jRepository.addRelationship(relationship, type);
                relationshipCounter++;
            }
            result.add("relationshipCounter", relationshipCounter);
        }
        return result;
    }

    @Override
    public MigrationData createInnerField(InputStream inputStream, MigrationData params) {
        MigrationData result = new MigrationData();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(delimiter);
            String sourceColumnType = (String) params.get("sourceColumnType");
            String valueColumnType = (String) params.get("valueColumnType");
            boolean unique = (boolean) params.get("unique");
            String fieldName = (String) params.get("fieldName");
            String sourceLabel = (String) params.get("sourceLabel");
            if (columnNames.length == 0) {
                throw new MigrationException("First row of dumped file must contain column names.");
            }
            for (String name : columnNames) {
                if (name.contains(" ")) {
                    throw new InvalidFieldException("Field name can not include spaces: " + name);
                }
            }
            int objectCounter = 0;
            Map<Object, List<Object>> fields = new HashMap<>();
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] values = data.split(delimiter);
                String id = values[0];
                String value = values[1];
                if (fields.containsKey(id)) {
                    List<Object> objects = fields.get(id);
                    objects.add(value);
                    fields.put(id, objects);
                } else {
                    List<Object> objects = new ArrayList<>();
                    objects.add(value);
                    fields.put(id, objects);
                }
                objectCounter++;
            }
            if (unique) {
                for (Object key : fields.keySet()) {
                    List<Object> values = fields.get(key);
                    fields.put(key, new ArrayList<>(new HashSet<>(values)));
                }
            }
            for (Object key : fields.keySet()) {
                Node node = new Node(columnNames[0], (String) key, sourceColumnType, sourceLabel);
                InnerField innerField = new InnerField(node, fieldName, valueColumnType, fields.get(key));
                neo4jRepository.addInnerField(innerField);
            }
            result.add("objectCounter", objectCounter);
        }
        return result;
    }

}
