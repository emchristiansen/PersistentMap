package object persistentmap {
  /**
   * This exception is raised when `PersistentMap` attempts to connect to a
   * backing table with an incompatible type.
   */
  class TableTypeException(message: String) extends RuntimeException(message)
}