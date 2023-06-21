package com.example.postgresneo4jmigrationtool.generator.uploader;

import java.io.InputStream;
import java.util.Map;

public interface Neo4jUploader {

    void upload(InputStream inputStream, Map<String, Object> params);

}
