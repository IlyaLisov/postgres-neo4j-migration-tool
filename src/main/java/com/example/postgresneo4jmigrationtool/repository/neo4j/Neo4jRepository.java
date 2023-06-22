package com.example.postgresneo4jmigrationtool.repository.neo4j;

import com.example.postgresneo4jmigrationtool.model.Node;

import java.util.Map;

public interface Neo4jRepository {

    void addNode(Node node, String... labels);

    void addRelationship(Object fromNodeId, Object toNodeId, Map<String, Object> data, String label);

}
