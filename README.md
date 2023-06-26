# Postgres-to-Neo4j migration tool

This is tool for database migration from Postgres to Neo4j.

This application can map tables from Postgres to nodes from Neo4j.

User can:

- choose what tables from database to migrate
- choose what columns from table to migrate
- rename columns
- add labels to generated nodes
- migrate relationships by migrating foreign keys
- reformat timestamp to time format

### How to use?

1) Fill `.env` file with your connection data
2) Configure XML script under `XML_CONFIG_LOCATION` path.
3) Run application

### Markup example

Here is an example of XML configuration file for node and relationship
migration.

You can validate your schema with `schema.xsd`schema.

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
                    <columnFrom>user_id</columnFrom>
                    <labelFrom>User</labelFrom>
                    <columnTo>task_id</columnTo>
                    <labelTo>Task</labelTo>
                </configuration>
                <type>HAS_TASK</type>
            </table>
        </tables>
    </relationship>
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
   will be stored as `<newName>` property;
10) `<timeFormat>` - (optional for `node` migration) format of timestamp to
    store in Neo4j. It is needed to store LocalDateTime and access it without
    converters in code.
11) `<labels>` - (optional for `node` migration) collection of labels to be
    added
    to Nodes.
12) `<label>` - label tag, defines its name.
13) `<columnFrom>` - column with foreign key to entity table. Relationship will
    be started from Node from that table by this foreign key.
14) `<columnTo>` - column with foreign key to entity table. Relationship will
    be ended with Node from that table by this foreign key.
15) `<labelFrom>` - (optional for `migration` migration) specifies label of
    start
    node to find it by
    foreign key.
16) `<labelTo>` - (optional for `migration` migration) specifies label of end
    node to
    find it by foreign
    key.
17) `<type>` - type of the relationship.

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

We handle Postgres types in generated JSON the following way:

- `integer`, `bigserial`, `biginteger` are considered numeric values
- `bool`, `boolean` are considered boolean values
- `timestamp`, `timestamp without time zone` as timestamp
- other types - strings.

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
them. It can be avoided but providing `<labelFrom>` and `<labelTo>` tags.

We parse timestamp from database then format it to provided time format (
from optional `<timeFormat>` tag).

If no exceptions were thrown, you will see messages in logs with amount of
created nodes and relationships.