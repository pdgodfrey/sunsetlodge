package us.pgodfrey.sunsetlodge.sql

data class SeasonSqlQueries(
  val getCurrentSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE) limit 1",
  val getNextSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE)+1 limit 1",

  val getSeasons: String = "select seasons.*," +
    "(id = (select id from seasons WHERE date_part('year', end_date) = date_part('year', CURRENT_DATE))) as is_current "+
    "from seasons order by start_date",

  val getSeason: String = "select seasons.*," +
    "(id = (select id from seasons WHERE date_part('year', end_date) = date_part('year', CURRENT_DATE))) as is_current "+
    "from seasons where id = $1",

  val createSeason: String = "insert into seasons (name, start_date, end_date, high_season_start_date, high_season_end_date, is_open ) values " +
    "($1, $2, $3, $4, $5, $6) returning *",
  val updateSeason: String = "update seasons set name = $1, start_date = $2, end_date = $3, high_season_start_date = $4, " +
    "high_season_end_date = $5, is_open = $6 where id = $7 returning *",
  val deleteSeason: String = "delete from seasons where id = $1",

  val getSeasonForDates: String = "select * from seasons where $1 between start_date and end_date"
)
