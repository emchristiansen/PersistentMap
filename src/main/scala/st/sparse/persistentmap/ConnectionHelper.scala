package st.sparse.persistentmap

import java.io.File
import scala.slick.session.Database

/**
 * Helper methods for connecting to common databases.
 * Because who can remember the particular syntax each database expects in
 * its connection strings?
 */
object ConnectionHelper {
  def jdbcURLSQLite: File => String = file => s"jdbc:sqlite:${file.toString}"
  def driverStringSQLite: String = "org.sqlite.JDBC"
  def databaseSQLite: File => Database = file =>
    Database.forURL(jdbcURLSQLite(file), driver = driverStringSQLite)

  def jdbcURLMySQL: (String, String, String, String) => String =
    (hostAndPort, database, user, password) =>
      s"jdbc:mysql://$hostAndPort/$database?user=$user&password=$password"
  def driverStringMySQL: String = "com.mysql.jdbc.Driver"
  def databaseMySQL: (String, String, String, String) => Database = {
    case args =>
      Database.forURL(
        jdbcURLMySQL.tupled(args),
        driver = driverStringMySQL)
  }

  def jdbcURLMariaDB: (String, String, String, String) => String =
    (hostAndPort, database, user, password) =>
      s"jdbc:mariadb://$hostAndPort/$database?user=$user&password=$password"
  def driverStringMariaDB: String = "org.mariadb.jdbc.Driver"
  def databaseMariaDB: (String, String, String, String) => Database = {
    case args =>
      Database.forURL(
        jdbcURLMariaDB.tupled(args),
        driver = driverStringMariaDB)
  }
}