package com.example.postgresneo4jmigrationtool.repository.neo4j;

import com.example.postgresneo4jmigrationtool.model.InnerField;
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
    public void addNode(Node node) {
        String query = "CREATE (n %s)";
        String data = node.toString();
        String preparedQuery = String.format(query, data);
        if (node.getLabels().length > 0) {
            String labels = " SET n: %s";
            labels = String.format(labels, String.join(" :", node.getLabels()));
            preparedQuery += labels;
        }
        neo4jClient.query(preparedQuery).fetch().all();
    }

    @Override
    @Transactional
    public void addRelationship(Relationship relationship, String type) {
        String query = "MATCH(nodeFrom %s %s) MATCH(nodeTo %s %s) CREATE (nodeFrom)-[:%s]->(nodeTo)";
        if (!relationship.getSource().getLabel().isEmpty()) {
            relationship.getSource().setLabel(": " + relationship.getSource().getLabel());
        }
        if (!relationship.getTarget().getLabel().isEmpty()) {
            relationship.getTarget().setLabel(": " + relationship.getTarget().getLabel());
        }
        String preparedQuery = String.format(query,
                relationship.getSource().getLabels()[0],
                relationship.getSource(),
                relationship.getTarget().getLabels()[0],
                relationship.getTarget(),
                type);
        neo4jClient.query(preparedQuery).fetch().all();
    }

    @Override
    @Transactional
    public void addInnerField(InnerField innerField) {
        String query = "MATCH(nodeFrom %s %s) SET nodeFrom.%s = %s";
        if (!innerField.getSource().getLabel().isEmpty()) {
            innerField.getSource().setLabel(": " + innerField.getSource().getLabel());
        }
        String preparedQuery = String.format(query,
                innerField.getSource().getLabel(),
                innerField.getSource(),
                innerField.getFieldName(),
                innerField);
        neo4jClient.query(preparedQuery).fetch().all();
    }

}
