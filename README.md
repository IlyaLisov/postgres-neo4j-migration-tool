# Postgres-to-Neo4j migration tool

This is tool for database migration from Postgres to Neo4j.

This application can map tables from Postgres to nodes from Neo4j.

User can:

- choose what tables from database to migrate
- choose what columns from table to migrate
- rename columns
- add labels to generated nodes

Future features:

- add relationships
- time and boolean formatting

### How to use?

1) Fill `.env` file with your connection data
2) Configure XML script under `XML_CONFIG_LOCATION` path.
3) Run application

### Markup example

Here is an example of XML configuration file.

```
<migration type="node">
    <tables>
        <table name="users">
            <configuration>
                <excludedColumns>
                    <column>surname</column>
                </excludedColumns>
                <renamedColumns>
                    <columns>
                        <previousName>name</previousName>
                        <newName>newName</newName>
                    </columns>
                </renamedColumns>
            </configuration>
            <labels>
                <label>User</label>
                <label>BaseEntity</label>
            </labels>
        </table>
        <table name="tasks"/>
    </tables>
</migration>
```

Tags:

1) `<migration>` - main tag, defines whether Node or Relationship is going to be
   created. You need to provide `type` attribute - `node`.
2) `<tables>` - collection of tables to be migrated.
3) `<table>` - table tag, defines table name in `name` attribute, its
   configuration and labels.
4) `<configuration>` - configuration for columns.
5) `<excludedColumns>` - columns to be excluded from migration. It means after
   migration in
   Neo4j no data from these columns will be stored.
6) `<column>` - column tag, contains table name.
7) `<renamedColumns>` - columns to be renamed during migration. It means data
   from column with `<previousName>`
   will be stored as `<newName>` property;
8) `<labels>` - collection of labels to be added to Nodes.
9) `<label>` - label tag, defines its name.

You can safely omit `<labels>` and / or `<configuration>` tags - then all
columns will be migrated and no labels will be added to generated nodes.

### How it works?

Migration is working by generating and executing script.
It dumps Postgres database to `*.csv` file in `/dump` folder, so app must have
access to write in current directory.
After it, these files are read and uploaded to Neo4j.

These migration files are not deleted after script execution. So you can see
what data was dumped and uploaded to Neo4j.

Note that at first we exclude columns and only after rename them. So if you will
rename excluded columns, it was excluded and no columns with this name will be
renamed.

If no exceptions were thrown, you will see messages in logs with amount of
created nodes.