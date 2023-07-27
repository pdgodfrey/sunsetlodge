package us.pgodfrey.sunsetlodge.sql

data class BookingSqlQueries(

  val getBookingsForSeason: String = "select bookings.*, seasons.name as season_name, " +
    "(select array_to_json(array_agg(row_to_json((building)))) from (select buildings.* from buildings " +
      "inner join bookings_buildings bb on bb.building_id = buildings.id "+
      "where bb.booking_id = bookings.id order by buildings.id) building) as buildings, " +
    "array(select building_id from bookings_buildings where booking_id = bookings.id) as building_ids " +
    "from bookings " +
    "inner join seasons on seasons.id = season_id " +
    "where season_id = $1 order by start_date",

  val createBooking: String = "insert into bookings (season_id, name, start_date, end_date) values " +
    "($1, $2, $3, $4) returning *",
  val updateBooking: String = "update bookings set name = $1, start_date = $2, end_date = $3 " +
    "where id = $4 returning *",
  val deleteBooking: String = "delete from bookings where id = $1",

  val createBookingBuilding: String = "insert into bookings_buildings (booking_id, building_id) values " +
    "($1, $2)",
  val deleteBookingBuilding: String = "delete from bookings_buildings where booking_id = $1 and building_id = $2",
  val deleteBookingBuildingForBooking: String = "delete from bookings_buildings where booking_id = $1"
)
