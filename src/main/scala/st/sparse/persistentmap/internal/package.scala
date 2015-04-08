package st.sparse.persistentmap

import scala.pickling.Defaults._
import scala.pickling._
import scala.pickling.binary._
import scala.slick.util.SlickLogger

package object internal {
//  type Logging = com.typesafe.scalalogging.slf4j.Logging
  type Logging = scala.slick.util.Logging

  def typeName[A: FastTypeTag] =
    implicitly[FastTypeTag[A]].tpe.toString

  def logPickle[A: Pickler: FastTypeTag](
    logger: SlickLogger,
    a: A) = {
    logger.debug(s"Pickling ${typeName[A]}.")
    a.pickle
  }

  def logUnpickle[A: Unpickler: FastTypeTag](
    logger: SlickLogger,
    pickle: BinaryPickle): A = {
    logger.debug(s"Unpickling ${typeName[A]}.")
    pickle.unpickle[A]
  }
}