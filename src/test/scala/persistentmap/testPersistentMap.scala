package persistentmap

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.FunSuite
import org.scalacheck.Gen
import scala.pickling._
import scala.pickling.binary._
import scala.slick.session.Database
import java.io.File

////////////////////////////////////////////////////////////////////////////////

case class MyKey(a: Int, b: String)
case class MyValue(a: MyKey, b: Double)

@RunWith(classOf[JUnitRunner])
class TestPersistentMap extends FunSuite with GeneratorDrivenPropertyChecks {
  test("acts like a map") {
    val database = {
      val tempFile = File.createTempFile("testPersistentMapDB", "sqlite")
      Database.forURL(s"jdbc:sqlite:$tempFile", driver = "org.sqlite.JDBC")
//      Database.forURL(s"jdbc:sqlite::memory", driver = "org.sqlite.JDBC")
    }
    //    val database = Database.forURL("jdbc:h2:db.h2", driver = "org.h2.Driver")
    //    val database = Database.forURL("jdbc:mariadb://localhost:3306/test", driver = "org.mariadb.jdbc.Driver")

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

  test("a vanilla unit test") {
    val x = 1
    assert(x == 1)
  }

  test("a generator driven test") {
    val evenInts = for (n <- Gen.choose(-1000, 1000)) yield 2 * n
    forAll(evenInts) { x =>
      assert(x % 2 == 0)
    }
  }
}
