import sbt._
import Keys._

object PersistentMapBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      "spray" at "http://repo.spray.io/"))

  val projectName = "PersistentMap"
  val mavenName = "persistent-map"

  val publishSettings = Seq(
    name := mavenName,

    version := "0.1.3-SNAPSHOT",

    organization := "st.sparse",

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    publishMavenStyle := true,

    publishArtifact in Test := false,

    pomIncludeRepository := { _ => false },

    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),

    homepage := Some(url("https://github.com/emchristiansen/PersistentMap")),

    pomExtra := (
      <scm>
        <url>git@github.com:emchristiansen/PersistentMap.git</url>
        <connection>scm:git:git@github.com:emchristiansen/PersistentMap.git</connection>
      </scm>
      <developers>
        <developer>
          <id>emchristiansen</id>
          <name>Eric Christiansen</name>
          <url>http://sparse.st</url>
        </developer>
      </developers>))

  val scalaVersionString = "2.11.6"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersionString,
      "com.typesafe.slick" %% "slick" % "2.1.0",
      "io.spray" %%  "spray-json" % "1.3.1",
			"org.scala-lang.modules" %% "scala-pickling" % "0.10.0",
      "joda-time" % "joda-time" % "2.7",
      "org.joda" % "joda-convert" % "1.7",
      // "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
//			"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "org.xerial" % "sqlite-jdbc" % "3.8.7" % "test",
//      "org.jumpmind.symmetric.jdbc" % "mariadb-java-client" % "1.1.1" % "test",
      "mysql" % "mysql-connector-java" % "5.1.35" % "test",
      "org.slf4j" % "slf4j-nop" % "1.7.12" % "test",
      "org.scalatest" %% "scalatest" % "3.0.0-SNAP4" % "test",
      "org.scalacheck" %% "scalacheck" % "1.12.2" % "test",
      "junit" % "junit" % "4.12" % "test"))

  def updateOnDependencyChange = Seq(
    watchSources <++= (managedClasspath in Test) map { cp => cp.files })

  def scalaSettings = Seq(
    scalaVersion := scalaVersionString,
    scalacOptions ++= Seq(
      "-optimize",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-language:implicitConversions",
      "-language:existentials",
      "-Xlog-implicits",
      // "-language:reflectiveCalls",
      "-language:postfixOps"))

  def moreSettings =
    Project.defaultSettings ++
      extraResolvers ++
      extraLibraryDependencies ++
      scalaSettings ++
      updateOnDependencyChange ++
      publishSettings

  lazy val root = {
    val settings = moreSettings ++ Seq(fork := true)
    Project(id = projectName, base = file("."), settings = settings)
  }
}
