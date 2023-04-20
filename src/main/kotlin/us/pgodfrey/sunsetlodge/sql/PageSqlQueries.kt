package us.pgodfrey.sunsetlodge.sql

data class PageSqlQueries(
  val currentSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE) limit 1",
  val nextSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE)+1 limit 1",

  val getHighSeasonRatesForSeason: String = "select buildings.name as building_name, max_occupancy, bedrooms, bathrooms, " +
    "high_season_rate from rates " +
    "inner join buildings on rates.building_id = buildings.id " +
    "where high_season_rate is not null and " +
    "season_id = $1",

  val getLowSeasonRatesForSeason: String = "select buildings.name as building_name, max_occupancy, bedrooms, bathrooms, " +
    "low_season_rate from rates " +
    "inner join buildings on rates.building_id = buildings.id " +
    "where season_id = $1"
)
