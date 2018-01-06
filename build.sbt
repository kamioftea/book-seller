name := """book-seller"""
organization := "uk.co.goblinoid"

version := "1.0"

sources in (Compile, doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.12.3"

libraryDependencies += guice
libraryDependencies += ws
libraryDependencies += jdbc
libraryDependencies += evolutions

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

libraryDependencies += "org.webjars.npm" % "foundation-sites" % "6.4.3"
libraryDependencies += "org.webjars" % "font-awesome" % "5.0.2"

libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.0"
libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.41"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "uk.co.goblinoid.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "uk.co.goblinoid.binders._"
