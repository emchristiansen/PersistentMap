package st.sparse.persistentmap

import scala.slick.session.Database
import scala.pickling.Unpickler
import scala.pickling.FastTypeTag
import scala.pickling.SPickler

class PersistentMemo[A, B] private (
  function: A => B,
  persistentMap: PersistentMap[A, B]) extends (A => B) {
  override def apply(a: A): B = persistentMap.getOrElse(a, {
    val b = function(a)
    persistentMap += a -> b
    b
  })
}

object PersistentMemo {
  def apply[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    database: Database,
    name: String,
    function: A => B)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): PersistentMemo[A, B] = {
    val persistentMap = PersistentMap.connectElseCreate[A, B](name, database)
    new PersistentMemo(function, persistentMap)
  }
}