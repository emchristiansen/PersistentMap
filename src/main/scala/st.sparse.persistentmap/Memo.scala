package st.sparse.persistentmap

import scala.slick.session.Database
import scala.pickling.Unpickler
import scala.pickling.FastTypeTag
import scala.pickling.SPickler
import st.sparse.persistentmap.internal.Logging
import spray.json._

/**
 * A function memoizer which can use `PersistentMap` for global, persistent
 * memoization.
 *
 * The scope of the memoization is controlled by the underlying map.
 * If you want global memoization of the function across multiple
 * call stacks / threads, use `PersistentMap`s (or `PersistentJsonMap`s)
 * which share the same table.
 * If you want local memoization (so to share memoization you need to pass
 * the `PersistentMemo` instance) use a `collection.mutable.Map`.
 */
case class Memo[A, B](
  function: A => B,
  map: collection.mutable.Map[A, B]) extends (A => B) with Logging {
  override def apply(a: A): B = map.get(a) match {
    case Some(b) =>
      logger.debug(s"Memo hit: $a => $b")
      b
    case None =>
      val b = function(a)
      logger.debug(s"Memo miss: $a => $b")
      map += a -> b
      b
  }
}

object Memo {
  /**
   *  Convenience function for when the backing map is a `PersistentMap`.
   */
  def create[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    name: String,
    database: Database,
    function: A => B)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): Memo[A, B] = Memo(
    function,
    PersistentMap.create[A, B](name, database))

  /**
   *  Convenience function for when the backing map is a `PersistentMap`.
   */
  def connect[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    name: String,
    database: Database,
    function: A => B)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): Option[Memo[A, B]] = {
    PersistentMap.connect[A, B](name, database).map(Memo(function, _))
  }

  /**
   *  Convenience function for when the backing map is a `PersistentMap`.
   */
  def connectElseCreate[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    name: String,
    database: Database,
    function: A => B)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): Memo[A, B] =
    connect[A, B](name, database, function).
      getOrElse(create[A, B](name, database, function))

  /**
   *  Convenience function for when the backing map is a `PersistentJsonMap`.
   */
  def createJson[A: JsonFormat: FastTypeTag, B: JsonFormat: FastTypeTag](
    name: String,
    database: Database,
    function: A => B): Memo[A, B] = Memo(
    function,
    PersistentJsonMap.create[A, B](name, database))

  /**
   *  Convenience function for when the backing map is a `PersistentJsonMap`.
   */
  def connectJson[A: JsonFormat: FastTypeTag, B: JsonFormat: FastTypeTag](
    name: String,
    database: Database,
    function: A => B): Option[Memo[A, B]] = {
    PersistentJsonMap.connect[A, B](name, database).map(Memo(function, _))
  }

  /**
   *  Convenience function for when the backing map is a `PersistentJsonMap`.
   */
  def connectElseCreateJson[A: JsonFormat: FastTypeTag, B: JsonFormat: FastTypeTag](
    name: String,
    database: Database,
    function: A => B): Memo[A, B] =
    connectJson[A, B](name, database, function).
      getOrElse(createJson[A, B](name, database, function))
}
