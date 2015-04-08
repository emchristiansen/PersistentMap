package st.sparse.persistentmap

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.FunSuite
import org.scalacheck.Gen
import scala.pickling._
import scala.pickling.binary._
import org.joda.time._

@RunWith(classOf[JUnitRunner])
class TestCustomPicklers extends FunSuite with GeneratorDrivenPropertyChecks {
  test("pickle DateTime") {
    val dateTime0 = new DateTime
    Thread.sleep(2000)
    val dateTime1 = new DateTime

    assert(dateTime0 != dateTime1)
    assert(dateTime0.pickle.unpickle[DateTime] == dateTime0)
    assert(dateTime1.pickle.unpickle[DateTime] == dateTime1)
  }

  test("pickle LocalDate") {
    val localDate = new LocalDate("2000-01-20")
    assert(localDate.pickle.unpickle[LocalDate] == localDate)
  }

  test("pickle BigDecimal") {
    val x: BigDecimal = 2.3

    assert(x.pickle.unpickle[BigDecimal] == x)
  }
}