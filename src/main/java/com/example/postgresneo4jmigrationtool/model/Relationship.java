package com.example.postgresneo4jmigrationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Relationship {

    private Node nodeFrom;
    private Node nodeTo;
    private String labelFrom;
    private String labelTo;

}
