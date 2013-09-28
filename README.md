#PersistentMap

`PersistentMap` is a type-safe, boilerplate-free, key-value store, built on top of [Slick](http://slick.typesafe.com/) and [scala-pickling](https://github.com/scala/pickling).
It exposes a new type, `PersistentMap[A, B]`, which extends `collection.mutable.Map[A, B]`.
You use a `PersistentMap` just like a regular mutable `Map`, and all changes to its state are automatically propagated to an underlying database.

Here's an example:

```scala
import scala.pickling._
import scala.pickling.binary._
import scala.slick.session.Database
import persistentmap._

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

See the tests for more details.

##Installation

This project is new, and so not yet on Sonatype.
You must currently do a `git clone` and an `sbt publish-local`.

[![Build Status](https://travis-ci.org/emchristiansen/PersistentMap.png)](https://travis-ci.org/emchristiansen/PersistentMap)

##A bit of commentary

The implementation of `PersistentMap` is absurdly simple, but its utility is obvious.
I hope a serious database project, e.g. Slick, picks up the idea.

##Known issues

* `org.xerial % sqlite-jdbc` is broken in OS X Mountain Lion, so the tests as written will fail in that OS.
If you're on Mountain Lion, you'll have to use a different database.
I've had luck with MariaDB.
* The table typechecking currently uses runtime reflection, which has documented thread safety issues.

##License

MIT / I don't care

