package st.sparse.persistentmap

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import spray.json.DefaultJsonProtocol._

import scala.slick.jdbc.JdbcBackend.Database

@RunWith(classOf[JUnitRunner])
class TestPersistentJsonMap extends FunSuite with GeneratorDrivenPropertyChecks {
  // Creates a SQLite database, backed by some random temporary file.
  def createSQLiteDatabase = {
    val tempFile = File.createTempFile("testPersistentMapDB", ".sqlite")
    ConnectionHelper.databaseSQLite(tempFile)
  }

  // These classes are currently defined in `testPersistentMap.scala`.
  implicit def myKeyFormat = jsonFormat2(MyKey.apply)
  implicit def myValueFormat = jsonFormat2(MyValue.apply)

  test("basic usage test") {
    val database: Database = createSQLiteDatabase

    val map = PersistentJsonMap.create[MyKey, MyValue]("test", database)

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
    val map2 = PersistentJsonMap.connect[MyKey, MyValue]("test", database).get

    assert(map == map2)
  }
}
