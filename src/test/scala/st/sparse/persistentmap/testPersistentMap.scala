package st.sparse.persistentmap

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.FunSuite
import org.scalacheck.Gen
import scala.pickling._
import scala.pickling.binary._
import scala.slick.session.Database
import java.io.File

case class MyKey(a: Int, b: String)
case class MyValue(a: MyKey, b: Double)

class Base(val y: Int)
case class Derived(val x: Int) extends Base(x)

@RunWith(classOf[JUnitRunner])
class TestPersistentMap extends FunSuite with GeneratorDrivenPropertyChecks {
  // Creates a MySQL database, backed by some random temporary file.
  def createSQLiteDatabase = {
    val tempFile = File.createTempFile("testPersistentMapDB", "sqlite")
    Database.forURL(s"jdbc:sqlite:$tempFile", driver = "org.sqlite.JDBC")
  }

  // This will only work in a Travis CI environment.
  def createMySQLDatabase = Database.forURL(
    "jdbc:mysql://localhost/myapp_test?user=\"travis\"&password=\"\"",
    driver = "com.mysql.jdbc.Driver")

  test("sample code") {
    val database: scala.slick.session.Database = createSQLiteDatabase

    // Create a `PersistentMap`.
    val map = PersistentMap.create[Int, String]("myMap", database)

    // Add key-value pairs.
    map += 1 -> "no"
    map += 2 -> "boilerplate"

    // Retrieve values.
    assert(map(1) == "no")

    // Delete key-value pairs.
    map -= 2

    // And do anything else supported by `collection.mutable.Map`.
  }

  test("test the core Map api with sample data") {
    def run(database: Database) = {
      val map = PersistentMap.create[MyKey, MyValue]("test", database)

      val key1 = MyKey(1, "one")
      val key2 = MyKey(2, "two")

      val value1 = MyValue(key1, 1.0)
      val value2 = MyValue(key2, 2.0)

      val kv11 = key1 -> value1
      val kv22 = key2 -> value2
      val kv12 = key1 -> value2

      assert(map.get(key1) == None)
      assert(map.get(key2) == None)
      assert(map.toSet == Set())

      map += kv11

      assert(map.get(key1) == Some(value1))
      assert(map.get(key2) == None)
      assert(map.toSet == Set(kv11))

      map += kv22

      assert(map.get(key1) == Some(value1))
      assert(map.get(key2) == Some(value2))
      assert(map.toSet == Set(kv11, kv22))

      map -= key1

      assert(map.get(key1) == None)
      assert(map.get(key2) == Some(value2))
      assert(map.toSet == Set(kv22))

      map += kv12

      assert(map.get(key1) == Some(value2))
      assert(map.get(key2) == Some(value2))
      assert(map.toSet == Set(kv12, kv22))

      // Now let's make sure this map is really persistent.
      val map2 = PersistentMap.connect[MyKey, MyValue]("test", database).get

      assert(map == map2)
    }

    run(createSQLiteDatabase)
    run(createMySQLDatabase)
  }

  // We ensure we can't connect to a table with the wrong type of data.
  test("basic table connection time type safety") {
    def run(database: Database) {
      PersistentMap.create[Int, Double]("test", database)

      // A `Double` is not a `String`, so this should fail to connect.
      intercept[TableTypeException] {
        PersistentMap.connect[Int, String]("test", database)
      }

      // Do a similar test with the key.
      intercept[TableTypeException] {
        PersistentMap.connect[String, Double]("test", database)
      }

      // For the sake of paranoia, make sure we can connect with the proper type.
      PersistentMap.connect[Int, Double]("test", database)
    }

    run(createSQLiteDatabase)
    run(createMySQLDatabase)
  }

  // We extract a base class from a `PersistentMap` of a derived class.
  test("table connection time type safety with subclasses") {
    def run(database: Database) {
      val map1 = PersistentMap.create[Int, Derived]("test", database)
      map1 += 42 -> Derived(10)

      val map2 = PersistentMap.connect[Int, Derived]("test", database).get
      assert(map1 == map2)

      // Retrieve the base type from a map with the derived type.
      val map3 = PersistentMap.connect[Int, Base]("test", database).get
      assert((map1(42): Base).y == map3(42).y)
    }
    
    run(createSQLiteDatabase)
    run(createMySQLDatabase)
  }
}
