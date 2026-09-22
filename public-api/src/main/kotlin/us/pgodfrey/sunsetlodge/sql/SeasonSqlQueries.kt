package us.pgodfrey.sunsetlodge.sql

data class SeasonSqlQueries(
  val getCurrentSeason: String = "select * from seasons where bookings_open_date <= CURRENT_DATE and is_closed is not true order by bookings_open_date limit 1",
  val getNextOpenSeason: String = "select * from seasons where bookings_open_date > CURRENT_DATE and is_closed is not true  order by bookings_open_date limit 1",
//  val getNextSeason: String = "select * from seasons where date_part('year', end_date) = date_part('year', CURRENT_DATE)+1 and is_open is true and id != $1 limit 1",
  val getNextSeason: String = "select * from seasons where bookings_open_date <= CURRENT_DATE and is_closed is not true and id != $1 order by bookings_open_date limit 1",
  val getNextAvailableSeason: String = "select * from seasons where date_part('year', end_date) > date_part('year', CURRENT_DATE) order by end_date limit 1",
  val getSeasonByName: String = "select * from seasons where name = $1",

  val getSeasons: String = "select * from (select seasons.*," +
    "(id = (select id from seasons where bookings_open_date <= CURRENT_DATE and is_closed is not true order by bookings_open_date desc limit 1)) as is_current "+
    "from seasons) tbl " +
    "order by is_current is not true, end_date < now()",

  val getSeason: String = "select seasons.*," +
    "(id = (select id from seasons where bookings_open_date <= CURRENT_DATE and is_closed is not true order by bookings_open_date desc limit 1)) as is_current "+
    "from seasons where id = $1",

  val createSeason: String = "insert into seasons (name, start_date, end_date, high_season_start_date, high_season_end_date, " +
    "bookings_open_date, sheet_rate, boat_package_rate, boat_separate_rate ) values " +
    "($1, $2, $3, $4, $5, $6, $7, $8, $9) returning *",
  val updateSeason: String = "update seasons set name = $1, start_date = $2, end_date = $3, high_season_start_date = $4, " +
    "high_season_end_date = $5, bookings_open_date = $6, sheet_rate = $7, boat_package_rate = $8, " +
    "boat_separate_rate = $9, is_closed = $10 where id = $11 returning *",
  val deleteSeason: String = "delete from seasons where id = $1",

  val getSeasonForDates: String = "select * from seasons where $1 between start_date and end_date",

  val getOpenSeasons: String = "select * from seasons where bookings_open_date <= CURRENT_DATE and is_closed is not true order by start_date limit 2"
)
