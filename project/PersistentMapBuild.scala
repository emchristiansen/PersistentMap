import sbt._
import Keys._

object PersistentMapBuild extends Build {
  def extraResolvers = Seq(
    resolvers ++= Seq(
      "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
      "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Local Maven Repository" at Path.userHome.asFile.toURI.toURL + "/.m2/repository",
      Resolver.url("emchristiansen-scalatest-extra", url("https://raw.github.com/emchristiansen/scalatest-extra/master/releases"))( Patterns("[organisation]/[module]/[revision]/[artifact]-[revision].[ext]") )
    )
  )

 val publishSettings = Seq(
    organization := "emchristiansen",
    publishMavenStyle := false,
    publishTo := Some(Resolver.file("file", new File("./releases"))),
    version := "0.1-SNAPSHOT")

  val scalaVersionString = "2.10.3-RC3"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersionString,
      "org.scala-lang" % "scala-compiler" % scalaVersionString,
      "commons-io" % "commons-io" % "2.4",
      "com.typesafe.slick" %% "slick" % "1.0.1",
      "org.scala-lang" %% "scala-pickling" % "0.8.0-SNAPSHOT",
      "org.xerial" % "sqlite-jdbc" % "3.7.2" % "test",
      "org.slf4j" % "slf4j-nop" % "1.6.4" % "test",
      "org.scalatest" %% "scalatest" % "2.0.M5b" % "test",
      "org.scalacheck" %% "scalacheck" % "1.10.1" % "test",
      "junit" % "junit" % "4.11" % "test"
    )
  )

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
      "-language:postfixOps"
    )
  )

  def moreSettings =
    Project.defaultSettings ++
    extraResolvers ++
    extraLibraryDependencies ++
    scalaSettings ++
    updateOnDependencyChange ++
    publishSettings

  val projectName = "PersistentMap"
  lazy val root = {
    val settings = moreSettings ++ Seq(name := projectName, fork := true)
    Project(id = projectName, base = file("."), settings = settings)
  }
}
