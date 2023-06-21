package com.example.postgresneo4jmigrationtool.generator.uploader;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;

@Service
public class CSVNeo4jUploader implements Neo4jUploader {

    @Override
    public void upload(InputStream inputStream, Map<String, Object> params) {

    }

}
