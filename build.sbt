name := "sp-cron"
organization := "com.signalpath"
version := "1.0"
scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "2.2.1",
  "com.github.alonsodomin.cron4s" %% "cron4s-core" % "0.6.0",
  "eu.timepit" %% "fs2-cron-core" % "0.2.2",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
)
