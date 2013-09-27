PersistentMap

This is a type-safe, boilerplate-free, key-value store, built on top of Slick and scala-pickling.
It exposes a new type, `PersistentMap[A, B]`, which extends `collection.mutable.Map[A, B]`.
You use a `PersistentMap` just like a regular mutable `Map`, and all changes to its state are automatically propagated to an underlying database.

Here's an example:

TODO

See the tests for more details.

A bit of commentary

The implementation of `PersistentMap` is absurdly simple, but its utility is obvious.
I hope a serious database project, e.g. Slick, picks up the idea.

Known issues

`org.xerial` % `sqlite-jdbc` is broken in OS X Mountain Lion, so the tests as written will fail in that OS.
If you're on Mountain Lion, you'll have to use a different database.
I've had luck with MariaDB.
