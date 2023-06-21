package com.example.postgresneo4jmigrationtool.generator.uploader;

import com.example.postgresneo4jmigrationtool.model.UploadParams;
import com.example.postgresneo4jmigrationtool.model.UploadResult;

import java.io.InputStream;

public interface Neo4jUploader {

    UploadResult upload(InputStream inputStream, UploadParams params);

}
