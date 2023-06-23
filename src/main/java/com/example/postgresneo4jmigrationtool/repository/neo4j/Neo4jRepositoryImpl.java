package com.example.postgresneo4jmigrationtool.repository.neo4j;

import com.example.postgresneo4jmigrationtool.model.Node;
import com.example.postgresneo4jmigrationtool.model.Relationship;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class Neo4jRepositoryImpl implements Neo4jRepository {

    private final Neo4jClient neo4jClient;

    @Override
    @Transactional
    public void addNode(Node node, String... labels) {
        String query = "CREATE (n %s)";
        String data = node.toString();
        String preparedQuery = String.format(query, data);
        if (labels.length > 0) {
            preparedQuery += " SET n :%s";
            preparedQuery = String.format(preparedQuery, String.join(" :", labels));
        }
        neo4jClient.query(preparedQuery).fetch().all();
    }

    @Override
    @Transactional
    public void addRelationship(Relationship relationship, String type) {
        String query = "MATCH(nodeFrom %s {%s: '%s'}) MATCH(nodeTo %s {%s: '%s'}) CREATE (nodeFrom)-[:%s]->(nodeTo)";
        if (!relationship.getLabelFrom().isEmpty()) {
            relationship.setLabelFrom(": " + relationship.getLabelFrom());
        }
        if (!relationship.getLabelTo().isEmpty()) {
            relationship.setLabelTo(": " + relationship.getLabelTo());
        }
        String preparedQuery = String.format(query,
                relationship.getLabelFrom(),
                relationship.getNodeFrom().getNames()[0],
                relationship.getNodeFrom().getValues()[0],
                relationship.getLabelTo(),
                relationship.getNodeTo().getNames()[0],
                relationship.getNodeTo().getValues()[0],
                type);
        neo4jClient.query(preparedQuery).fetch().all();
    }

}
