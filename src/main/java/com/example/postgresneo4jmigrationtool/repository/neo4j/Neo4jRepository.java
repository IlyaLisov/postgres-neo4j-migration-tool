package com.example.postgresneo4jmigrationtool.repository.neo4j;

import java.util.Map;

public interface Neo4jRepository {

    void addNode(Map<String, Object> data, String... labels);

    void addRelationship(Object fromNodeId, Object toNodeId, Map<String, Object> data, String label);

}
