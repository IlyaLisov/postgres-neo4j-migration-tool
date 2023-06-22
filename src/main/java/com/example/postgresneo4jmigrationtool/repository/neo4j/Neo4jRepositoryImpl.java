package com.example.postgresneo4jmigrationtool.repository.neo4j;

import com.example.postgresneo4jmigrationtool.model.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class Neo4jRepositoryImpl implements Neo4jRepository {

    private final Neo4jClient neo4jClient;

    @Override
    @Transactional
    public void addNode(Node node, String... labels) {
        String query = "CREATE (n %s)";
        String data = node.getDataString();
        String preparedQuery = String.format(query, data);
        if (labels.length > 0) {
            preparedQuery += " SET n :%s";
            preparedQuery = String.format(preparedQuery, String.join(" :", labels));
        }
        neo4jClient.query(preparedQuery).fetch().all();
    }

    @Override
    @Transactional
    public void addRelationship(Object fromNodeId, Object toNodeId, Map<String, Object> data, String label) {

    }

}
