package st.sparse.persistentmap

import scala.pickling._
import scala.pickling.binary._
import scala.slick.session.Database
import scala.slick.session.Session
import scala.slick.jdbc._
import StaticQuery.interpolation
import scala.slick.jdbc.meta.MTable
import scala.slick.session.PositionedParameters
import scala.slick.session.PositionedResult
import internal._

// This is used to encode the type of a `PersistentMap` in a table.
private case class TypeRecord(keyType: String, valueType: String)

// This is used to store the actual data in a `PersistentMap` in a table.
private case class KeyValueRecord[A, B](keyHash: Int, key: A, value: B)

// This is used below to minimize the chance of problems from lack of
// reflection thread safety.
private object RuntimeReflectionLock

// These implicits come from:
// https://github.com/slick/slick/issues/97
// With any luck this object will be removed when the issue is resolved.
private object PersistentMapImplicits {
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
  recordsTableName: String) extends collection.mutable.Map[A, B] with Logging {
  import PersistentMapImplicits._

  // These implicits are required to unpack results from SQL queries
  // into Scala objects.
  private implicit val getTypeRecordResult =
    GetResult(r => TypeRecord(r.nextString, r.nextString))

  private implicit val getKeyValueRecordResult =
    GetResult(r => KeyValueRecord(
      r.nextInt,
      logUnpickle[A](logger, BinaryPickle(r.nextBytes)),
      logUnpickle[B](logger, BinaryPickle(r.nextBytes))))

  // Do consistency checks on the two backing tables.
  // The properties checked here are assumed to be invariant throughout
  // the existence of this map.
  database withSession { implicit session: Session =>
    // The type table:
    // 1) must exist,
    require(MTable.getTables(typeTableName).list().size == 1)
    val entries = sql"select * from #$typeTableName;".as[TypeRecord].list
    // 2) must have exactly one entry,
    require(entries.size == 1)

    // Asserts `derived` is a subtype of `Base`, where `Base` is an
    // actual type, and `derived` is a string representing a type.
    // This uses runtime reflection, which is probably not the best solution,
    // because, among other things, it is not thread safe:
    // http://docs.scala-lang.org/overviews/reflection/thread-safety.html
    // Thus we're synchronizing inside this function.
    // IF YOU KNOW A BETTER WAY PLEASE LET ME KNOW.
    def assertSubtype[Base: FastTypeTag](derived: String, component: String) {
      // To check for a subtype relationship, we simply attempt to compile
      // some indicator code.
      // Compilation succeeds iff the subtype relationship holds.
      val baseString = typeName[Base]
      val code = s"(x: $derived) => x: $baseString"

      RuntimeReflectionLock.synchronized {
        import scala.reflect.runtime._
        val cm = universe.runtimeMirror(getClass.getClassLoader)
        import scala.tools.reflect.ToolBox
        val tb = cm.mkToolBox()

        try {
          tb.compile(tb.parse(code))
        } catch {
          case _: scala.tools.reflect.ToolBoxError =>
            throw new TableTypeException(
              s"Expected $component type was $baseString but type in " +
                s"underlying table is $derived. Did you connect to " +
                "the wrong table?")
        }
      }

    }

    // 3) and that entry must reflect the required type.
    assertSubtype[A](entries.head.keyType, "key")
    assertSubtype[B](entries.head.valueType, "value")

    // The records table must exist.
    require(MTable.getTables(recordsTableName).list().size == 1)

    // We also assume the index on the records table exists, but I don't know
    // how to check for that.
  }

  private def hashKey(key: A): Int =
    key.hashCode

  override def get(key: A): Option[B] = {
    database withSession { implicit session: Session =>
      // We look up the key-value pair using the key's hash code.
      // This may have false positives.
      val keyHashHits =
        sql"select * from #$recordsTableName where keyHash = ${hashKey(key)};".
          as[KeyValueRecord[A, B]].list

      // Here we weed out the false positives.
      val list = keyHashHits.filter(_.key == key)

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
      sql"select * from #$recordsTableName;".as[KeyValueRecord[A, B]].list.
        toIterator map {
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

      val keyBytes = logPickle(logger, key).value
      val valueBytes = logPickle(logger, value).value
      sqlu"insert into #$recordsTableName values(${hashKey(key)}, $keyBytes, $valueBytes);".first
    }

    this
  }

  override def -=(key: A): this.type = {
    database withSession { implicit session: Session =>
      val keyBytes = logPickle(logger, key).value      
      sqlu"delete from #$recordsTableName where keyHash = ${hashKey(key)} and keyData = $keyBytes;".first
    }

    this
  }
}

object PersistentMap {
  private def typeTable(name: String): String = name + "TypeTable"
  private def recordsTable(name: String): String = name + "RecordsTable"
  private def recordsTableIndex(name: String): String =
    name + "RecordsTableIndex"

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
    val recordsTableIndexName = recordsTableIndex(name)

    database withSession { implicit session: Session =>
      // Initialize the type table.      
      if (!MTable.getTables(typeTableName).elements.isEmpty)
        sqlu"drop table #$typeTableName;".first

      // `text` might be a more natural choice than `varchar`, but it seems
      // to cause problems in other parts of the code.
      // So, we have this hack where we assume no type strings will be longer
      // than 10k characters.
      sqlu"create table #$typeTableName(keyType varchar(10000) not null, valueType varchar(10000) not null);".first

      val aString = typeName[A]
      val bString = typeName[B]
      sqlu"insert into #$typeTableName values($aString, $bString);".first

      // Initialize the records table.
      if (!MTable.getTables(recordsTableName).elements.isEmpty) {
        // We assume the index also exists, so we delete it as well.
        // In fact, we delete it first, otherwise it would be dangling at some
        // point, and something crazy might happen.
        sqlu"drop index #$recordsTableIndexName on #$recordsTableName;".first
        sqlu"drop table #$recordsTableName;".first
      }

      // We want to do quick lookups using the key hash, but we can't make it
      // a primary key due to the possibility of hash collisions.
      // Instead, we build an index on the key hash.
      sqlu"create table #$recordsTableName(keyHash int not null, keyData blob not null, valueData blob not null);".first
      sqlu"create index #$recordsTableIndexName on #$recordsTableName(keyHash);".first
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
      val typeTableExists =
        !MTable.getTables(typeTableName).elements.isEmpty
      val recordsTableExists =
        !MTable.getTables(recordsTableName).elements.isEmpty

      // Make sure either both exist or neither exists.
      // If just one exists, something funky is up.
      assert(typeTableExists && recordsTableExists ||
        (!typeTableExists && !recordsTableExists))

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