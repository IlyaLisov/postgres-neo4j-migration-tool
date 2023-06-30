package com.example.postgresneo4jmigrationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class MigrationData {

    private Map<String, Object> params;

    public MigrationData() {
        this.params = new HashMap<>();
    }

    public void add(String key, Object object) {
        params.put(key, object);
    }

    public Object get(String key) {
        return params.get(key);
    }

}
