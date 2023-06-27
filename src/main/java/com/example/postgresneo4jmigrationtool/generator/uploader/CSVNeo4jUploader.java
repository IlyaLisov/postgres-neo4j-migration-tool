package com.example.postgresneo4jmigrationtool.generator.uploader;

import com.example.postgresneo4jmigrationtool.model.InnerField;
import com.example.postgresneo4jmigrationtool.model.Node;
import com.example.postgresneo4jmigrationtool.model.Relationship;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidConfigurationException;
import com.example.postgresneo4jmigrationtool.model.exception.InvalidFieldException;
import com.example.postgresneo4jmigrationtool.model.exception.MigrationException;
import com.example.postgresneo4jmigrationtool.repository.neo4j.Neo4jRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
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

    @Override
    public UploadResult createNode(InputStream inputStream, UploadParams params) {
        UploadResult uploadResult = new UploadResult();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(String.valueOf(params.get("delimiter")));
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
                String[] values = data.split(String.valueOf(params.get("delimiter")));
                Node node = new Node(columnNames, values, types.toArray(new String[0]));
                node.setTimeFormat(timeFormat);
                neo4jRepository.addNode(node, labels.toArray(new String[0]));
                nodeCounter++;
            }
            uploadResult.add("nodeCounter", nodeCounter);
        }
        return uploadResult;
    }

    @Override
    public UploadResult createRelationship(InputStream inputStream, UploadParams params) {
        UploadResult uploadResult = new UploadResult();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(String.valueOf(params.get("delimiter")));
            String columnFromType = (String) params.get("columnFromType");
            String columnToType = (String) params.get("columnToType");
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
            String labelFrom = (String) params.get("labelFrom");
            String labelTo = (String) params.get("labelTo");
            int relationshipCounter = 0;
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] values = data.split(String.valueOf(params.get("delimiter")));
                Node nodeFrom = new Node(columnNames[0], values[0], columnFromType);
                Node nodeTo = new Node(columnNames[1], values[1], columnToType);
                Relationship relationship = new Relationship(nodeFrom, nodeTo, labelFrom, labelTo);
                neo4jRepository.addRelationship(relationship, type);
                relationshipCounter++;
            }
            uploadResult.add("relationshipCounter", relationshipCounter);
        }
        return uploadResult;
    }

    @Override
    public UploadResult createInnerField(InputStream inputStream, UploadParams params) {
        UploadResult uploadResult = new UploadResult();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(String.valueOf(params.get("delimiter")));
            String columnFromType = (String) params.get("columnFromType");
            String valueColumnType = (String) params.get("valueColumnType");
            boolean unique = (boolean) params.get("unique");
            String fieldName = (String) params.get("fieldName");
            String labelFrom = (String) params.get("labelFrom");
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
                String[] values = data.split(String.valueOf(params.get("delimiter")));
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
                Node node = new Node(columnNames[0], (String) key, columnFromType);
                InnerField innerField = new InnerField(node, labelFrom, fieldName, valueColumnType, fields.get(key));
                neo4jRepository.addInnerField(innerField);
            }
            uploadResult.add("objectCounter", objectCounter);
        }
        return uploadResult;
    }

}
