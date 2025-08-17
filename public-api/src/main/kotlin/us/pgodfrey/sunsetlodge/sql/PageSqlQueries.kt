package us.pgodfrey.sunsetlodge.sql

data class PageSqlQueries(

  val getHighSeasonRatesForSeason: String = "select buildings.name as building_name, max_occupancy, bedrooms, bathrooms, " +
    "high_season_rate from rates " +
    "inner join buildings on rates.building_id = buildings.id " +
    "where high_season_rate is not null and " +
    "season_id = $1",

  val getLowSeasonRatesForSeason: String = "select buildings.name as building_name, max_occupancy, bedrooms, bathrooms, " +
    "low_season_rate from rates " +
    "inner join buildings on rates.building_id = buildings.id " +
    "where season_id = $1",

  val getBookingsForSeason: String = "select bookings.*, " +
    "ARRAY(select identifier from buildings " +
      "inner join bookings_buildings bb on bb.building_id = buildings.id " +
      "where booking_id = bookings.id) as buildings from bookings where season_id = $1",

)
