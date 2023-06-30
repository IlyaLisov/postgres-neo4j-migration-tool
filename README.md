# Postgres-to-Neo4j migration tool

This is tool for database migration from Postgres to Neo4j.

This application can map tables from Postgres to nodes from Neo4j.

User can:

- choose what tables from database to migrate
- choose what columns from table to migrate
- rename columns
- add labels to generated nodes
- migrate relationships by migrating foreign keys
- reformat timestamp to custom time format
- migrate tables as inner fields

### How to use?

1) Fill `.env` file with your connection data
2) Configure XML script under `XML_CONFIG_LOCATION` path.
3) Run application

### Markup example

Here is an example of XML configuration file for node and relationship
migration.

You can validate your schema with default `schema.xsd` schema. You need to
pass `XML_VALIDATION_ENABLED` property as `true`.

```
<migration>
    <node>
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
                    <follow>
                        <column value="OneType">dtype</column>
                    </follow>
                    <skip>
                        <column value="AnotherType">dtype</column>
                    </skip>
                    <timeFormat>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timeFormat>
                </configuration>
                <labels>
                    <label>User</label>
                    <label>BaseEntity</label>
                </labels>
            </table>
            <table name="tasks">
                <labels>
                    <label>Task</label>
                    <label>BaseEntity</label>
                </labels>
            </table>
        </tables>
    </node>
    <relationship>
        <tables>
            <table name="users_tasks">
                <configuration>
                    <sourceColumn>user_id</sourceColumn>
                    <sourceLabel>User</sourceLabel>
                    <targetColumn>task_id</targetColumn>
                    <targetLabel>Task</targetLabel>
                    <follow>
                        <column value="OneType">dtype</column>
                    </follow>
                    <skip>
                        <column value="AnotherType">dtype</column>
                    </skip>
                </configuration>
                <type>HAS_TASK</type>
            </table>
        </tables>
    </relationship>
    <innerField>
        <tables>
            <table name="users_roles">
                <configuration>
                    <sourceColumn>user_id</sourceColumn>
                    <sourceLabel>User</sourceLabel>
                    <valueColumn>role</valueColumn>
                    <fieldName>userRole</fieldName>
                    <unique>true</unique>
                </configuration>
            </table>
        </tables>
    </innerField>
</migration>
```

1) `<migration>` - main tag, contains what Node or/and Relationship is going to
   be created.
2) `<node>` - (optional) node migration description tag.
3) `<relationship>` - (optional) relationship migration description tag.
4) `<tables>` - collection of tables to be migrated.
5) `<table>` - table tag, defines table name in `name` attribute, its
   configuration and labels.
6) `<configuration>` - (optional for `node` migration) configuration for
   columns.
7) `<excludedColumns>` - (optional for `node` migration) columns to be excluded
   from migration. It
   means after migration in Neo4j no data from these columns will be stored.
8) `<column>` - column tag, contains table name.
9) `<renamedColumns>` - (optional for `node` migration) columns to be renamed
   during migration. It means data from column with `<previousName>`
   will be stored as `<newName>` property.
10) `<follow>` - (optional for `node`, `relationship` migration) follows only
    that rows which cells in these columns are equal to `value` attribute
    of `<column>` tag. If multiple columns are provided, all columns match is
    required to migrate it.
11) `<skip>` - (optional for `node`, `relationship` migration) skips only
    that rows which cells in these columns are equal to `value` attribute
    of `<column>` tag. If multiple columns are provided, at least one match is
    required to skip it.
12) `<timeFormat>` - (optional for `node` migration) format of timestamp to
    store in Neo4j. It is needed to store LocalDateTime and access it without
    converters in code.
13) `<labels>` - (optional for `node` migration) collection of labels to be
    added
    to Nodes.
14) `<label>` - label tag, defines its name.
15) `<sourceColumn>` - column with foreign key to entity table. Relationship
    will
    be started from Node from that table by this foreign key. Inner field will
    be added to node with this primary key.
16) `<targetColumn>` - column with foreign key to entity table. Relationship
    will
    be ended with Node from that table by this foreign key.
17) `<sourceLabel>` - (optional for `relationship`, `innerField` migration)
    specifies
    label of
    start
    node to find it by
    foreign key.
18) `<targetLabel>` - (optional for `relationship` migration) specifies label of
    end
    node to
    find it by foreign
    key.
19) `<type>` - type of the relationship.
20) `<valueColumn>` - name of column with value for inner field migration.
21) `<fieldName>` - name of inner field of node to set value to.
22) `<unique>` - (optional for `innerField` migration) specify whether values in
    inner field must be unique. False if not present.

### NOTE

You can safely omit `<labels>` and / or `<configuration>` tags for node
migration - then all
columns will be migrated and no labels will be added to generated nodes.

If you want to migrate relationships, you need to add labels to ensure type of
nodes to be connected. You can omit this tag if you sure that all of your nodes
have unique id.

Note that at first we exclude columns and only after rename them. So if you will
rename excluded columns, it was excluded and no columns with this name will be
renamed.

We first handle `<skip>` rows, it means if row matches `<skip>` rule and it
matches `<follow>` rule, it won`t be migrated.

By providing several values for one column, they are considered as array of
available values. If this array contains cell value, `<skip>` rule will skip
this row, `<follow>` rule will follow this row.

We handle Postgres types in generated JSON the following way:

- `integer`, `bigserial`, `biginteger` are considered numeric values
- `bool`, `boolean` are considered boolean values
- `timestamp`, `timestamp without time zone` as timestamp
- other types - strings.

If there are `null` in cell, we store this as `null` too, so it won`t be saved
to
node.

We recommend to fill up all tags to be sure that correct data will
be saved to Neo4j.

### How it works?

Migration is working by generating and executing script.
It dumps Postgres database to `*.csv` file in `/dump` folder, so app must have
access to write in current directory.
After it, these files are read and uploaded to Neo4j.

These migration files are not deleted after script execution. So you can see
what data was dumped and uploaded to Neo4j.

Relationship migration is provided by matching nodes with provided primary key.
So if some of your nodes have similar id, relationship will be added to each of
them. It can be avoided but providing `<sourceLabel>` and `<targetLabel>` tags.

We parse timestamp from database then format it to provided time format (
from optional `<timeFormat>` tag).

If no exceptions were thrown, you will see messages in logs with amount of
created nodes and relationships.