package com.example.postgresneo4jmigrationtool.generator.uploader;

import com.example.postgresneo4jmigrationtool.model.Node;
import com.example.postgresneo4jmigrationtool.model.Relationship;
import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;
import com.example.postgresneo4jmigrationtool.repository.neo4j.Neo4jRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class CSVNeo4jUploader implements Neo4jUploader {

    private final Neo4jRepository neo4jRepository;

    @Override
    public UploadResult uploadNode(InputStream inputStream, UploadParams params) {
        UploadResult uploadResult = new UploadResult();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(String.valueOf(params.get("delimeter")));
            String datePattern = (String) params.get("datePattern");
            Map<String, String> newNames = (Map<String, String>) params.get("newNames");
            List<String> labels = (List<String>) params.get("labels");
            for (int i = 0; i < columnNames.length; i++) {
                if (newNames.containsKey(columnNames[i])) {
                    columnNames[i] = newNames.get(columnNames[i]);
                }
            }
            int nodeCounter = 0;
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] values = data.split(String.valueOf(params.get("delimeter")));
                Node node = new Node(columnNames, values);
                neo4jRepository.addNode(node, labels.toArray(new String[0]));
                nodeCounter++;
            }
            uploadResult.add("nodeCounter", nodeCounter);
        }
        return uploadResult;
    }

    @Override
    public UploadResult uploadRelationship(InputStream inputStream, UploadParams params) {
        UploadResult uploadResult = new UploadResult();
        try (Scanner scanner = new Scanner(inputStream)) {
            String headers = scanner.nextLine();
            String[] columnNames = headers.split(String.valueOf(params.get("delimeter")));
            String type = (String) params.get("type");
            String labelFrom = (String) params.get("labelFrom");
            String labelTo = (String) params.get("labelTo");
            int relationshipCounter = 0;
            while (scanner.hasNextLine()) {
                String data = scanner.nextLine();
                String[] values = data.split(String.valueOf(params.get("delimeter")));
                Node nodeFrom = new Node(new String[]{columnNames[0]}, new String[]{values[0]});
                Node nodeTo = new Node(new String[]{columnNames[1]}, new String[]{values[1]});
                Relationship relationship = new Relationship(nodeFrom, nodeTo, labelFrom, labelTo);
                neo4jRepository.addRelationship(relationship, type);
                relationshipCounter++;
            }
            uploadResult.add("relationshipCounter", relationshipCounter);
        }
        return uploadResult;
    }
}
