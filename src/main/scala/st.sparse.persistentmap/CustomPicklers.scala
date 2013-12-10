package st.sparse.persistentmap

import org.joda.time._
import scala.pickling._

/**
 * This contains workarounds for a few cases where scala-pickling fails.
 *
 * You must import the contents of this object to have access to 
 * the workarounds.
 * Obviously, this isn't a complete list.
 * Hopefully, though, scala-pickling will become stable soon and this whole
 * file can be deleted.
 */
object CustomPicklers {
  // TODO: Remove this custom pickler when the following issue is resolved:
  // https://github.com/scala/pickling/issues/33
  class CustomDateTimePickler(implicit val format: PickleFormat)
    extends SPickler[DateTime] with Unpickler[DateTime] {
    def pickle(picklee: DateTime, builder: PBuilder) {
      builder.beginEntry(picklee)
      builder.putField("iso8601", b =>
        b.hintTag(FastTypeTag.ScalaString).beginEntry(picklee.toString()).endEntry())
      builder.endEntry()
    }

    def unpickle(tag: => FastTypeTag[_], reader: PReader): DateTime = {
      reader.beginEntry()
      val date = reader.readField("iso8601").unpickle[String]
      reader.endEntry()
      new DateTime(date)
    }
  }

  implicit def customDateTimePickler(implicit format: PickleFormat) = new CustomDateTimePickler()

  class CustomLocalDatePickler(implicit val format: PickleFormat)
    extends SPickler[LocalDate] with Unpickler[LocalDate] {
    def pickle(picklee: LocalDate, builder: PBuilder) {
      builder.beginEntry(picklee)
      builder.putField("LocalDate", b =>
        b.hintTag(FastTypeTag.ScalaString).beginEntry(picklee.toString()).endEntry())
      builder.endEntry()
    }

    def unpickle(tag: => FastTypeTag[_], reader: PReader): LocalDate = {
      reader.beginEntry()
      val date = reader.readField("LocalDate").unpickle[String]
      reader.endEntry()
      new LocalDate(date)
    }
  }

  implicit def customLocalDatePickler(implicit format: PickleFormat) = new CustomLocalDatePickler()

  class CustomBigDecimalPickler(implicit val format: PickleFormat)
    extends SPickler[BigDecimal] with Unpickler[BigDecimal] {
    def pickle(picklee: BigDecimal, builder: PBuilder) {
      builder.beginEntry(picklee)
      builder.putField("BigDecimal", b =>
        b.hintTag(FastTypeTag.ScalaString).beginEntry(picklee.toString()).endEntry())
      builder.endEntry()
    }

    def unpickle(tag: => FastTypeTag[_], reader: PReader): BigDecimal = {
      reader.beginEntry()
      val string = reader.readField("BigDecimal").unpickle[String]
      reader.endEntry()
      BigDecimal(string)
    }
  }

  implicit def customBigDecimalPickler(implicit format: PickleFormat) = new CustomBigDecimalPickler()
}