package us.pgodfrey.sunsetlodge.sql

data class MiscSqlQueries(
  val getBuildings: String = "select * from buildings order by id",
  val getRoles: String = "select * from roles order by name desc",
)
