package com.github.music.of.the.ainur.almaren.builder.core

import com.github.music.of.the.ainur.almaren.Tree
import com.github.music.of.the.ainur.almaren.builder.Core
import com.github.music.of.the.ainur.almaren.state.core.{SourceJdbc, SourceSql, SourceFile, SourceDataFrame}
import org.apache.spark.sql.{DataFrame, Dataset}

private[almaren] trait Source extends Core {
  def sourceSql(sql: String): Option[Tree] =
    SourceSql(sql)

  def sourceJdbc(url: String, driver: String, query: String, user: Option[String] = None, password: Option[String] = None, params: Map[String, String] = Map()): Option[Tree] =
    SourceJdbc(url, driver, query, user, password, params)

  def sourceFile(format: String, path: String, params: Map[String, String] = Map()): Option[Tree] =
    SourceFile(format,path,params)

  def sourceDataFrame[T](ds: Dataset[T]): Option[Tree] = 
    SourceDataFrame(ds.toDF())
}
