#PersistentMap

PersistentMap is a type-safe, boilerplate-free, key-value store, built on top of [Slick](http://slick.typesafe.com/) and [scala-pickling](https://github.com/scala/pickling).
Unlike existing key-value stores, it does not require the user to manually specify the database schema; instead, serialization is done automatically using scala-pickling.
It exposes a new type, `PersistentMap[A, B]`, which extends `collection.mutable.Map[A, B]`.
You use a `PersistentMap` just like a regular mutable `Map`, and all changes to its state are automatically propagated to an underlying database.

Here's an example:

```scala
import scala.pickling._
import scala.pickling.binary._
import scala.slick.session.Database
import st.sparse.persistentmap._
// Optionally include workarounds for a few cases where scala-pickling
// currently fails.
// import CustomPicklers._

val database: scala.slick.session.Database = ...
    
// Create a `PersistentMap`.
// Of course, you can also connect to an existing one.
val map = PersistentMap.create[Int, String]("myMap", database)
    
// Add key-value pairs.
map += 1 -> "no"
map += 2 -> "boilerplate"
    
// Retrieve values.
assert(map(1) == "no")
    
// Delete key-value pairs.
map -= 2
    
// And do anything else supported by `collection.mutable.Map`.
...

// You can use arbitrary types in the store, so long as scala-pickling
// can handle them.

case class Foo(int: Int)
case class Bar(foos: List[Foo], string: String)

val otherMap = PersistentMap.create[Foo, Bar]("myOtherMap", database)
  
otherMap += Foo(10) -> Bar(List(Foo(1), Foo(2)), "hello")
```

As demonstrated in the above code, scala-pickling can handle existing case classes, but it is by no means limited to that.
See Heather Miller's [talk](http://www.parleys.com/play/51c3799fe4b0d38b54f4625a/chapter0/about) ([slides](https://speakerdeck.com/heathermiller/on-pickles-and-spores-improving-support-for-distributed-programming-in-scala)) for more information.

For more detailed information on using PersistentMap, see the [simple example project](http://github.com/emchristiansen/PersistentMapExample) or the tests.

##Installation

You can use PersistentMap in your SBT project by simply adding the following dependency to your build file:

```scala
libraryDependencies += "st.sparse" %% "persistent-map" % "0.1-SNAPSHOT"
```

You also need to add the Sonatype "snapshots" repository resolver to your build file:

```scala
resolvers += Resolver.sonatypeRepo("snapshots")
```

[![Build Status](https://travis-ci.org/emchristiansen/PersistentMap.png)](https://travis-ci.org/emchristiansen/PersistentMap)

##PersistentMap vs Slick

Slick is the premier (or at least the TypeSafe-backed) database interface for Scala.
Like PersistentMap, Slick provides type-safety.
However, Slick requires some boilerplate when defining tables, especially when the table stores records of an existing type.
PersistentMap requires no such boilerplate, for the reason scala-pickling requires no boilerplate.

##A bit of commentary

The implementation of PersistentMap is absurdly simple, but its utility is obvious.
I hope a serious database project picks up the idea.

##Known issues

* `org.xerial % sqlite-jdbc` is broken in OS X Mountain Lion, so the tests as written will fail in that OS.
If you're on Mountain Lion, you'll have to use a different database.
I've had luck with MariaDB.
* Unless you have MySQL set up the way Travis CI expects, the MySQL tests will fail.
Travis CI's MySQL environment is explained here: http://about.travis-ci.org/docs/user/database-setup/.
* The table typechecking currently uses runtime reflection, which has documented thread safety issues.
For this reason, table typechecking currently lives in a synchronized block.
Hopefully this solves the issue, though in my experience unexpected things can happen with concurrent runtime reflection.

##License

MIT / I don't care

