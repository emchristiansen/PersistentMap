#PersistentMap

PersistentMap is a type-safe, boilerplate-free, key-value store, built on top of [Slick](http://slick.typesafe.com/) and [scala-pickling](https://github.com/scala/pickling).
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
```

Please also see the [simple example project](http://github.com/emchristiansen/PersistentMapExample) or the tests for more details.

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

Slick is premier (or at least the TypeSafe-backed) database interface for Scala.
Like PersistentMap, Slick provides type-safety.
However, Slick requires some boilerplate when defining tables, especially when the table stores records of an existing type.
PersistentMap requires no such boilerplate, for the reason scala-pickling requires no boilerplate.

##A bit of commentary

The implementation of PersistentMap is absurdly simple, but its utility is obvious.
I hope a serious database project, e.g. Slick, picks up the idea.

##Known issues

* `org.xerial % sqlite-jdbc` is broken in OS X Mountain Lion, so the tests as written will fail in that OS.
If you're on Mountain Lion, you'll have to use a different database.
I've had luck with MariaDB.
* The table typechecking currently uses runtime reflection, which has documented thread safety issues.

##License

MIT / I don't care

