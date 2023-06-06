package us.pgodfrey.sunsetlodge.sql

data class RateSqlQueries(
  val getRatesForSeason: String = "select rates.*, seasons.name as season_name, buildings.name as building_name from rates " +
    "inner join seasons on season_id = seasons.id " +
    "inner join buildings on building_id = buildings.id " +
    "where season_id = $1 order by building_id",

  val getRate: String = "select rates.* from rates where id = $1",

  val getRateForSeasonAndBuilding: String = "select rates.* from rates where season_id = $1 and building_id = $2",

  val createRate: String = "insert into rates (season_id, building_id, high_season_rate, low_season_rate) values " +
    "($1, $2, $3, $4) returning *",
  val updateRate: String = "update rates set high_season_rate = $1, low_season_rate = $1 where id = $3 returning *",
  val deleteRate: String = "delete from rates where id = $1"
)
