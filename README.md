# Almaren Framework

[![Build Status](https://travis-ci.org/music-of-the-ainur/almaren-framework.svg?branch=master)](https://travis-ci.org/music-of-the-ainur/almaren-framework)
[![Slack](https://img.shields.io/badge/chat-Slack-blue)](https://musicoftheainur.slack.com/messages/CN959DA2F)

The Almaren Framework provides an interface layered over Apache Spark. It does all the hard work using an elegant and minimalistic syntax while still allowing you to take advantage of native Apache Spark features. You can still combine it with standard Spark code.

## Components

### Source

#### sourceSql

Read native Spark/Hive tables using Spark SQL.

#### sourceHbase

Read from Hbase using [HBase Connector](https://github.com/hortonworks-spark/shc)

#### sourceCassandra

Read from Cassandra using [Spark Cassandra Connector](https://github.com/datastax/spark-cassandra-connector)

#### SourceJdbc

Read from JDBC using [Spark JDBC](https://spark.apache.org/docs/latest/sql-data-sources-jdbc.html)

### Core

#### Cache

Cache/Uncache both DataFrame or Table

#### Coalesce

Decrease the number of partitions in the RDD to numPartitions. Useful for running operations more efficiently after filtering down a large dataset.

### Repartition

Reshuffle the data in the RDD randomly to create either more or fewer partitions and balance it across them. This always shuffles all data over the network.

### Pipe

Pipe each partition of the RDD through a shell command, e.g. a Perl or bash script. RDD elements are written to the process's stdin and lines output to its stdout are returned as an RDD of strings.

#### Deserializer

Deserialize data structures like XML, JSON, Avro etc to Spark DataFrame

#### SQL Statement

[Spark SQL](https://docs.databricks.com/spark/latest/spark-sql/index.html) syntax. You can query preview component through the special table __TABLE__.

### Target

#### targetSql

Write native Spark/Hive tables using Spark SQL.

#### targetHbase

Write to Hbase using [HBase Connector](https://github.com/hortonworks-spark/shc)

#### targetCassandra

Write to Cassandra using [Spark Cassandra Connector](https://github.com/datastax/spark-cassandra-connector)

#### TargetJdbc

Write to JDBC using [Spark JDBC](https://spark.apache.org/docs/latest/sql-data-targets-jdbc.html)

## Examples

### Example 1

![Example 1](https://raw.githubusercontent.com/music-of-the-ainur/almaren-framework/master/docs/images/example1.png)

```scala
val almaren = Almaren("appName")
val df:DataFrame = almaren.sourceSql("SELECT * FROM db.schema.table")
    .deserializer("JSON","json_str")
    .dsl("uuid$id:StringType
        |code$area_code:LongType
        |names@name
        |	name.firstName$first_name:StringType
        |	name.secondName$second_name:StringType
        |	name.lastName$last_name:StringType
        |source_id$source_id:LongType".stripMargin)
    .sql("""SELECT *,unix_timestamp() as timestamp from __TABLE__""")
    .targetSql("INSERT OVERWRITE TABLE default.target_table SELECT * FROM __TABLE__")
    .batch
```

### Example 2

![Example 2](https://raw.githubusercontent.com/music-of-the-ainur/almaren-framework/master/docs/images/example2.png)

```scala
val almaren = Almaren("appName")
val sourceData = almaren.sourceSql("SELECT * FROM db.schema.table")
    .deserializer("XML","xml_str").cache.fork
        
sourceData.dsl("uuid$id:StringType
    |code$area_code:LongType
    |names@name
    |    name.firstName$first_name:StringType
    |    name.secondName$second_name:StringType
    |    name.lastName$last_name:StringType
    |source_id$source_id:LongType".stripMargin)
.sql("SELECT *,unix_timestamp() as timestamp from __TABLE__")
.targetCassandra("test1","kv1")
    
sourceData.dsl("uuid$id:StringType
    |code$area_code:LongType
    |phones@phone
    |    phone.number$phone_number:StringType
    |source_id$source_id:LongType".stripMargin)
.sql("SELECT *,unix_timestamp() as timestamp from __TABLE__")
.targetCassandra("test2","kv2")

sourceData.batch
```

### Example 3

![Example 3](https://raw.githubusercontent.com/music-of-the-ainur/almaren-framework/master/docs/images/example3.png)

```scala
val almaren = Almaren("appName")

val sourcePolicy = almaren.sourceHbase("""{
    |"table":{"namespace":"default", "name":"policy"},
    |"rowkey":"id",
    |"columns":{
    |"rowkey":{"cf":"rowkey", "col":"id", "type":"long"},
    |"number":{"cf":"Policy", "col":"number", "type":"long"},
    |"source":{"cf":"Policy", "col":"source", "type":"string"},
    |"status":{"cf":"Policy", "col":"status", "type":"string"},
    |"person_id":{"cf":"Policy", "col":"source", "type":"long"}
    |}
|}""").sql(""" SELECT * FROM __TABLE__ WHERE status = "ACTIVE" """).alias("policy")

val sourcePerson = almaren.sourceHbase("""{
    |"table":{"namespace":"default", "name":"person"},
    |"rowkey":"id",
    |"columns":{
    |"rowkey":{"cf":"rowkey", "col":"id", "type":"long"},
    |"name":{"cf":"Policy", "col":"number", "type":"string"},
    |"type":{"cf":"Policy", "col":"type", "type":"string"},
    |"age":{"cf":"Policy", "col":"source", "type":"string"}
    |}
|}""").sql(""" SELECT * FROM __TABLE__ WHERE type = "PREMIUM" """).alias("person")

almaren.sql(""" SELECT * FROM person JOIN policy ON policy.person_id = person.id """)
    .sql("SELECT *,unix_timestamp() as timestamp FROM __TABLE__")
    .coalesce(100)
    .targetSql("INSERT INTO TABLE area.premimum_users SELECT * FROM __TABLE__")
    .batch
```

### Example 4

![Example 4](https://raw.githubusercontent.com/music-of-the-ainur/almaren-framework/master/docs/images/example4.png)

```scala
val almaren = Almaren("appName")
val sourceData = almaren.sourceJdbc("jdbc:oracle:thin:@localhost:1521:xe","SELECT * FROM schema.table WHERE st_date >= (sysdate-1) AND st_date < sysdate")
    .sql("SELECT to_json(*) from __TABLE__")
    .coalesce(30)
    .targetRest("https://host.com:9093/api/foo","post",Map("Authorization" -> "Basic QWxhZGRpbjpPcGVuU2VzYW1l"))
    
sourceData.batch
```
