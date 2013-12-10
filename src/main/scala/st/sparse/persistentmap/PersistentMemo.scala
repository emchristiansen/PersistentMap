package st.sparse.persistentmap

import scala.slick.session.Database
import scala.pickling.Unpickler
import scala.pickling.FastTypeTag
import scala.pickling.SPickler
import st.sparse.persistentmap.internal.Logging

class PersistentMemo[A, B] private (
  function: A => B,
  persistentMap: PersistentMap[A, B]) extends (A => B) with Logging {
  override def apply(a: A): B = persistentMap.get(a) match {
    case Some(b) =>
      logger.debug(s"PersistentMemo hit: $a => $b")
      b
    case None =>
      val b = function(a)
      logger.debug(s"PersistentMemo miss: $a => $b")
      persistentMap += a -> b
      b
  }
}

object PersistentMemo {
  def apply[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    database: Database,
    name: String,
    function: A => B)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): PersistentMemo[A, B] = {
    val mapName = name + "Memo"
    val persistentMap = PersistentMap.connectElseCreate[A, B](mapName, database)
    new PersistentMemo(function, persistentMap)
  }
}