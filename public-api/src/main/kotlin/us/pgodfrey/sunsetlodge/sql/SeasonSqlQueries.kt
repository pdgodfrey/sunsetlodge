package us.pgodfrey.sunsetlodge.sql

data class SeasonSqlQueries(
  val getCurrentSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE) limit 1",
  val getNextSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE)+1 limit 1",

  val getSeasons: String = "select * from (select seasons.*," +
    "(id = (select id from seasons WHERE date_part('year', end_date) = date_part('year', CURRENT_DATE))) as is_current "+
    "from seasons) tbl " +
    "order by is_current is not true, end_date < now()",

  val getSeason: String = "select seasons.*," +
    "(id = (select id from seasons WHERE date_part('year', end_date) = date_part('year', CURRENT_DATE))) as is_current "+
    "from seasons where id = $1",

  val createSeason: String = "insert into seasons (name, start_date, end_date, high_season_start_date, high_season_end_date, " +
    "is_open, sheet_rate, boat_package_rate, boat_separate_rate ) values " +
    "($1, $2, $3, $4, $5, $6, $7, $8, $9) returning *",
  val updateSeason: String = "update seasons set name = $1, start_date = $2, end_date = $3, high_season_start_date = $4, " +
    "high_season_end_date = $5, is_open = $6, sheet_rate = $7, boat_package_rate = $8, " +
    "boat_separate_rate = $9 where id = $10 returning *",
  val deleteSeason: String = "delete from seasons where id = $1",

  val getSeasonForDates: String = "select * from seasons where $1 between start_date and end_date",

  val getOpenSeasons: String = "select * from seasons where is_open is true and end_date > now() order by start_date limit 2"
)
