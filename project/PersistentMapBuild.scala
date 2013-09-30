import sbt._
import Keys._

object PersistentMapBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"))

  val projectName = "PersistentMap"
  val mavenName = "persistent-map"

  val publishSettings = Seq(
    name := mavenName,

    version := "0.1-SNAPSHOT",

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
        <url>git@github.com:emchristiansne/PersistentMap.git</url>
        <connection>scm:git:git@github.com:emchristiansen/PersistentMap.git</connection>
      </scm>
      <developers>
        <developer>
          <id>emchristiansen</id>
          <name>Eric Christiansen</name>
          <url>http://sparse.st</url>
        </developer>
      </developers>))

  val scalaVersionString = "2.10.3-RC3"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersionString,
      "com.typesafe.slick" %% "slick" % "1.0.1",
      "org.scala-lang" %% "scala-pickling" % "0.8.0-SNAPSHOT",
      "org.xerial" % "sqlite-jdbc" % "3.7.2" % "test",
      "org.slf4j" % "slf4j-nop" % "1.6.4" % "test",
      "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
      "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
      "junit" % "junit" % "4.11" % "test"))

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
