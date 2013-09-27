package persistentmap

import scala.pickling._
import scala.pickling.binary._
import scala.slick.session.Database
import scala.slick.session.Session
import scala.slick.jdbc._
import StaticQuery.interpolation
import scala.slick.jdbc.meta.MTable
import scala.slick.session.PositionedParameters
import scala.slick.session.PositionedResult

////////////////////////////////////////////////////////////////////////////////

case class TypeRecord(keyType: String, valueType: String)

case class KeyValueRecord[A, B](keyHash: Long, key: A, value: B)

object PersistentMapImplicits {
  // These implicits come from
  // https://github.com/slick/slick/issues/97
  implicit object SetByteArray extends SetParameter[Array[Byte]] {
    def apply(v: Array[Byte], pp: PositionedParameters) {
      pp.setBytes(v)
    }
  }

  implicit object SetByteArrayOption extends SetParameter[Option[Array[Byte]]] {
    def apply(v: Option[Array[Byte]], pp: PositionedParameters) {
      pp.setBytesOption(v)
    }
  }

  implicit object GetByteArray extends GetResult[Array[Byte]] {
    def apply(rs: PositionedResult) = rs.nextBytes()
  }

  implicit object GetByteArrayOption extends GetResult[Option[Array[Byte]]] {
    def apply(rs: PositionedResult) = rs.nextBytesOption()
  }
}

/**
 * A mutable map which is backed by a database.
 *
 * `database` holds the table in which this map is stored.
 * `typeTableName` is the name of the type table, which is used to ensure
 * the records in the record table can be unpickled to the required type.
 * `recordsTableName` is the name of the records table, which
 * holds the actual data composing this map.
 * _These tables must already exist_; see `PersistentMap.create`
 * to create a new map.
 */
class PersistentMap[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
  database: Database,
  typeTableName: String,
  recordsTableName: String)(
    implicit ftt2a: FastTypeTag[FastTypeTag[A]],
    ftt2b: FastTypeTag[FastTypeTag[B]]) extends collection.mutable.Map[A, B] {
  import PersistentMapImplicits._

  // These implicits are required to unpack results from SQL queries
  // into Scala objects.
  private implicit val getTypeRecordResult =
    GetResult(r => TypeRecord(r.nextString, r.nextString))

  private implicit val getKeyValueRecordResult =
    GetResult(r => KeyValueRecord(
      r.nextLong,
      BinaryPickle(r.nextBytes).unpickle[A],
      BinaryPickle(r.nextBytes).unpickle[B]))

  // Do consistency checks on the two backing tables.
  // The properties checked here are assumed to be invariant throughout
  // the existence of this map.
  database withSession { implicit session: Session =>
    // The type table:
    // 1) must exist,
    require(MTable.getTables(typeTableName).list().size == 1)
    val entries = sql"select * from #$typeTableName".as[TypeRecord].list
    // 2) must have exactly one entry,
    require(entries.size == 1)
    // 3) and that entry must reflect the required type.
    // TODO: Implement once FastTypeTag deserialization works.
    // https://github.com/scala/pickling/issues/32
    //    require(entries.head.keyType == implicitly[FastTypeTag[A]].toString)
    //    require(entries.head.valueType == implicitly[FastTypeTag[B]].toString,
    //      s"${entries.head.valueType} == ${implicitly[FastTypeTag[B]].toString}")

    // The records table must exist.
    require(MTable.getTables(recordsTableName).list().size == 1)
  }

  private def hashKey(key: A): Long =
    // TODO: Make this return a proper `Long`, otherwise this is going
    // to be a problem eventually
    key.hashCode

  override def get(key: A): Option[B] = {
    database withSession { implicit session: Session =>
      // We look up the key-value pair using the key's hash code.
      val list =
        sql"select * from #$recordsTableName where keyHash = ${hashKey(key)}".as[KeyValueRecord[A, B]].list

      // We should get either zero or one result.
      assert(list.size == 0 || list.size == 1)

      list.headOption map { _.value }
    }
  }

  override def iterator: Iterator[(A, B)] = {
    database withSession { implicit session: Session =>
      // We simply return all the database entries, dropping the hash codes.
      // TODO: Make this a proper enumerator by either using a newer
      // version of Slick or by using the workaround described here:
      // http://stackoverflow.com/questions/16728545/nullpointerexception-when-plain-sql-and-string-interpolation
      sql"select * from #$recordsTableName".as[KeyValueRecord[A, B]].list.toIterator map {
        record => (record.key, record.value)
      }
    }
  }

  override def +=(kv: (A, B)): this.type = {
    database withSession { implicit session: Session =>
      val (key, value) = kv
      // If this key is in the database already, we delete its record.
      this -= key

      // Then we insert our new record.
      sqlu"insert into #$recordsTableName values(${hashKey(key)}, ${key.pickle.value}, ${value.pickle.value})".first
    }

    this
  }

  override def -=(key: A): this.type = {
    database withSession { implicit session: Session =>
      sqlu"delete from #$recordsTableName where keyHash = ${hashKey(key)}".first
    }

    this
  }
}

object PersistentMap {
  private def typeTable(name: String): String = name + "TypeTable"
  private def recordsTable(name: String): String = name + "RecordsTable"

  /**
   * Creates a new map.
   *
   * If another map of the same name already exists, this will clobber it.
   */
  def create[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    name: String,
    database: Database)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): PersistentMap[A, B] = {
    import PersistentMapImplicits._

    val typeTableName = typeTable(name)
    val recordsTableName = recordsTable(name)

    database withSession { implicit session: Session =>
      // Initialize the type table.      
      if (!MTable.getTables(typeTableName).elements.isEmpty)
        sqlu"drop table #$typeTableName".first

      // TODO: Store type tags, not strings.
      //      sqlu"create table #$typeTableName(keyType varchar not null, valueType varchar not null)".first
      sqlu"create table #$typeTableName(keyType blob not null, valueType blob not null)".first

      //      val aString = implicitly[FastTypeTag[A]].toString
      //      val bString = implicitly[FastTypeTag[B]].toString
      //      sqlu"insert into #$typeTableName values($aString, $bString)".first
      val aTypeBlob = implicitly[FastTypeTag[A]].pickle.value
      val bTypeBlob = implicitly[FastTypeTag[B]].pickle.value
      sqlu"insert into #$typeTableName values($aTypeBlob, $bTypeBlob)".first

      // Initialize the records table.
      if (!MTable.getTables(recordsTableName).elements.isEmpty)
        sqlu"drop table #$recordsTableName".first

      sqlu"create table #$recordsTableName(keyHash bigint not null primary key, keyData blob not null, valueData blob not null)".first
    }

    // Build the final map.
    new PersistentMap[A, B](database, typeTableName, recordsTableName)
  }

  /**
   * Attempts to connect to an existing map, returning None on failure.
   */
  def connect[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    name: String,
    database: Database)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): Option[PersistentMap[A, B]] = {
    val typeTableName = typeTable(name)
    val recordsTableName = recordsTable(name)

    database withSession { implicit session: Session =>
      val typeTableExists = !MTable.getTables(typeTableName).elements.isEmpty
      val recordsTableExists = !MTable.getTables(recordsTableName).elements.isEmpty

      // Make sure either both exist or neither exists.
      // If just one exists, something funky is up.
      // TODO
      //      assert(typeTableExists xand recordTableExists)

      if (typeTableExists && recordsTableExists)
        Some(new PersistentMap[A, B](database, typeTableName, recordsTableName))
      else None
    }
  }

  /**
   * Attempts to connect to a map, and if it fails, creates a new map.
   */
  def connectElseCreate[A: SPickler: Unpickler: FastTypeTag, B: SPickler: Unpickler: FastTypeTag](
    name: String,
    database: Database)(
      implicit ftt2a: FastTypeTag[FastTypeTag[A]],
      ftt2b: FastTypeTag[FastTypeTag[B]]): PersistentMap[A, B] =
    connect[A, B](name, database).getOrElse(create[A, B](name, database))
}