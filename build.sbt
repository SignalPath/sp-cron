name := "sp-cron"
organization := "com.signalpath"
version := "1.0-SNAPSHOT"

lazy val versions = new {
  val scala212 = "2.12.11"
  val scala213 = "2.13.7"
  val supportedScalaVersions = List(scala212, scala213)
}

crossScalaVersions := versions.supportedScalaVersions

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "3.0.4",
  "com.github.alonsodomin.cron4s" %% "cron4s-core" % "0.6.1",
  "eu.timepit" %% "fs2-cron-cron4s" % "0.7.1",
  "org.typelevel" %% "cats-core" % "2.6.1",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test,
  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
)

bintrayOrganization := Some("signalpath")
bintrayRepository := "scala"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
