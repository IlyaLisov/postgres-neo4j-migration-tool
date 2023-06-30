package com.example.postgresneo4jmigrationtool.generator.uploader;

import com.example.postgresneo4jmigrationtool.model.MigrationData;

import java.io.InputStream;

public interface Neo4jUploader {

    MigrationData createNode(InputStream inputStream, MigrationData params);

    MigrationData createRelationship(InputStream inputStream, MigrationData params);

    MigrationData createInnerField(InputStream inputStream, MigrationData params);

}
