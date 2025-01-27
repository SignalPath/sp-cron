name := "sp-cron"
organization := "com.signalpath"
version := "2.0"

lazy val versions = new {
  val scala212 = "2.12.20"
  val scala213 = "2.13.15"
  val supportedScalaVersions = List(scala212, scala213)
}

crossScalaVersions := versions.supportedScalaVersions

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "2.5.10",
  "com.github.alonsodomin.cron4s" %% "cron4s-core" % "0.6.1", // tied to fs2-cron-cron4s below
  "eu.timepit" %% "fs2-cron-cron4s" % "0.5.0", // code needs to be reworked if we want to up these
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalamock" %% "scalamock" % "6.1.1" % Test,
)

bintrayOrganization := Some("signalpath")
bintrayRepository := "scala"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
