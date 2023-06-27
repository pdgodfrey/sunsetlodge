package us.pgodfrey.sunsetlodge.sql

data class UserSqlQueries(

  val getUsers: String = "select users.*, roles.name as role_name from users " +
    "inner join roles on role_id = roles.id " +
    "order by users.name",

  val insertUser: String = "insert into users (name, email, role_id,  reset_token, reset_token_expiration, created_at, updated_at) " +
    "values ($1, $2, 2, now() + interval '10 minutes', now(), now())",

  val updateUser: String = "update users set name = $1, email = $2, role_id = $3, updated_at = $4 where id = $5",

  val setResetPassword: String = "update users set reset_token = $1, reset_token_expiration = now() + interval '10 minutes'," +
    "updated_at = now() where id = $2",

  val setPassword: String = "update users set password = $1, reset_token = null, reset_token_expiration = null where "+
    "id = $2",

  val getUserById: String = "select users.*, roles.name as role_name from users " +
    "inner join roles on role_id = roles.id " +
    "where users.id = $1",
  val getUserByEmail: String = "select users.*, roles.name as role_name from users " +
    "inner join roles on role_id = roles.id " +
    "where email = $1",
  val getUserByResetToken: String = "select * from users where reset_token = $1"

)
