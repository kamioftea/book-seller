name := """book-seller"""
organization := "uk.co.goblinoid"

version := "1.0"

sources in(Compile, doc) := Seq.empty
publishArtifact in(Compile, packageDoc) := false

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += jdbc
libraryDependencies += evolutions

//noinspection SpellCheckingInspection
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

libraryDependencies += "org.webjars.npm" % "foundation-sites" % "6.4.3"
libraryDependencies += "org.webjars" % "font-awesome" % "5.0.2"

//noinspection SpellCheckingInspection
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.0"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.41"

libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.1"
libraryDependencies += "com.typesafe.slick" %% "slick-codegen" % "3.2.1"

libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.12.6-play26"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.9" % Test,
  "com.typesafe.akka" %% "akka-stream" % "2.5.9",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.9" % Test,
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.9"
)

val slickGen = taskKey[Seq[File]]("Build slick tables")
slickGen := genTables((sourceManaged in Compile).value)

sourceGenerators in Compile += slickGen

def genTables(base: File): Seq[File] = {
  import slick.codegen.SourceCodeGenerator
  import com.typesafe.config.{Config, ConfigFactory}

  val outputDir = base.getPath
  val config: Config = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()
  val default = config.getConfig("slick.dbs.default")

  println(default.getString("db.driver").replace("$", ""))

  SourceCodeGenerator.main(
    Array(
      default.getString("profile").replace("$", ""),
      default.getString("db.driver"),
      default.getString("db.url"),
      outputDir,
      "db.books",
      default.getString("db.user"),
      default.getString("db.password")
    )
  )

  Seq(base / "db" / "books" / "Tables.scala")
}
