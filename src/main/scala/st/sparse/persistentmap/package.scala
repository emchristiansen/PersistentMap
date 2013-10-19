package st.sparse

import scala.pickling._
import scala.pickling.binary._
import com.typesafe.scalalogging.slf4j.Logger

package object persistentmap {
  /**
   * This exception is raised when `PersistentMap` attempts to connect to a
   * backing table with an incompatible type.
   */
  class TableTypeException(message: String) extends RuntimeException(message)

  private[persistentmap]type Logging = com.typesafe.scalalogging.slf4j.Logging

  private[persistentmap] def typeName[A: FastTypeTag] =
    implicitly[FastTypeTag[A]].tpe.toString

  private[persistentmap] def logPickle[A: SPickler: FastTypeTag](
    logger: Logger,
    a: A) = {
    logger.debug(s"Pickling ${typeName[A]}.")
    a.pickle
  }
  
  private[persistentmap] def logUnpickle[A: Unpickler: FastTypeTag](
    logger: Logger,
    pickle: BinaryPickle): A = {
    logger.debug(s"Unpickling ${typeName[A]}.")
    pickle.unpickle[A]
  }
}