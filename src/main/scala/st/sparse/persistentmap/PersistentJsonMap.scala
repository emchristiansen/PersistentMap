package st.sparse.persistentmap

import st.sparse.persistentmap.internal._
import spray.json._
import scala.slick.session.Database
import scala.pickling.FastTypeTag

/**
 * A PersistentMap which uses spray-json to do the bulk of the serialization,
 * leaving only string serialization to scala-pickling.
 *
 * This is useful for when scala-pickling isn't working properly; as a
 * new library using advanced Scala features, it can be quite touchy.
 * To use this class, you will need to provide `JsonFormat` instances for the
 * types you want to use.
 * Also realize data is stored internally using JSON, so it could use a lot
 * of space.
 */
class PersistentJsonMap[A: JsonFormat, B: JsonFormat] private (
  persistentMap: PersistentMap[String, String]) extends collection.mutable.Map[A, B] with Logging {
  def toJson[A: JsonFormat](a: A): String = a.toJson.compactPrint
  def fromJson[A: JsonFormat](string: String): A = string.asJson.convertTo[A]

  override def get(key: A): Option[B] =
    persistentMap.get(toJson(key)).map(fromJson[B])

  override def iterator: Iterator[(A, B)] = persistentMap.iterator.map {
    case (a, b) => (fromJson[A](a), fromJson[B](b))
  }

  override def +=(kv: (A, B)): this.type = {
    persistentMap += toJson(kv._1) -> toJson(kv._2)
    this
  }

  override def -=(key: A): this.type = {
    persistentMap -= toJson(key)
    this
  }
}

object PersistentJsonMap {
  private def appendTypeInfo[A: FastTypeTag, B: FastTypeTag](
    name: String): String = {
    val a = typeName[A].filter(_.isLetterOrDigit)
    val b = typeName[B].filter(_.isLetterOrDigit)
    name + "_" + a + "_" + b
  }

  def create[A: JsonFormat: FastTypeTag, B: JsonFormat: FastTypeTag](
    name: String,
    database: Database): PersistentJsonMap[A, B] = {
    val persistentMap = PersistentMap.create[String, String](
      appendTypeInfo[A, B](name),
      database)
    new PersistentJsonMap(persistentMap)
  }

  def connect[A: JsonFormat: FastTypeTag, B: JsonFormat: FastTypeTag](
    name: String,
    database: Database): Option[PersistentJsonMap[A, B]] = {
    val persistentMapOption = PersistentMap.connect[String, String](
      appendTypeInfo[A, B](name),
      database)
    persistentMapOption.map(new PersistentJsonMap(_))
  }

  def connectElseCreate[A: JsonFormat: FastTypeTag, B: JsonFormat: FastTypeTag](
    name: String,
    database: Database): PersistentJsonMap[A, B] =
    connect[A, B](name, database).getOrElse(create[A, B](name, database))
}