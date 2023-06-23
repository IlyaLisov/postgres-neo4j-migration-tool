package com.example.postgresneo4jmigrationtool.repository.neo4j;

import com.example.postgresneo4jmigrationtool.model.Node;
import com.example.postgresneo4jmigrationtool.model.Relationship;

public interface Neo4jRepository {

    void addNode(Node node, String... labels);

    void addRelationship(Relationship relationship, String type);

}
