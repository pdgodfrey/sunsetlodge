package us.pgodfrey.sunsetlodge.sql

data class AuthSqlQueries(

  val getLatestRefreshTokenForUser: String = "select * from refresh_tokens where user_id = $1 order by refresh_token_expiration desc limit 1",
  val setRefreshTokenToUsed: String = "update refresh_tokens set is_used = true where refresh_token = $1",
  val revokeRefreshTokensForUser: String = "update refresh_tokens set is_used = true where user_id = $1",
  val revokePreviousRefreshTokensForUser: String = "update refresh_tokens set is_used = true where user_id = $1 and refresh_token != $2",
  val getRefreshToken: String = "select * from refresh_tokens where refresh_token = $1",
  val insertRefreshToken: String = "insert into refresh_tokens (refresh_token, refresh_token_expiration, user_id) values " +
    "($1, $2, $3)"

)
