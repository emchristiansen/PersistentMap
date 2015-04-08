package st.sparse.persistentmap

import org.slf4j.Logger

import scala.pickling._
import scala.pickling.binary._

package object internal {
//  type Logging = com.typesafe.scalalogging.slf4j.Logging
  type Logging = scala.slick.util.Logging

  def typeName[A: FastTypeTag] =
    implicitly[FastTypeTag[A]].tpe.toString

  def logPickle[A: SPickler: FastTypeTag](
    logger: Logger,
    a: A) = {
    logger.debug(s"Pickling ${typeName[A]}.")
    a.pickle
  }

  def logUnpickle[A: Unpickler: FastTypeTag](
    logger: Logger,
    pickle: BinaryPickle): A = {
    logger.debug(s"Unpickling ${typeName[A]}.")
    pickle.unpickle[A]
  }
}