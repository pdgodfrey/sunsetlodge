package us.pgodfrey.sunsetlodge.sql

data class MiscSqlQueries(
  val getBuildings: String = "select * from buildings order by id",
)
