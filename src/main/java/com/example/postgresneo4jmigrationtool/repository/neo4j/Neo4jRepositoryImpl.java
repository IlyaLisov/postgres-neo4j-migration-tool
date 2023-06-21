package com.example.postgresneo4jmigrationtool.repository.neo4j;

import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class Neo4jRepositoryImpl implements Neo4jRepository {

    @Override
    public void addNode(Map<String, Object> data, String... labels) {

    }

    @Override
    public void addRelationship(Object fromNodeId, Object toNodeId, Map<String, Object> data, String label) {

    }

}
