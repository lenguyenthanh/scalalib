import sbt._
import Keys._

object ScalalibBuild extends Build {

  lazy val core = Project("core", file(".")) settings (
    organization := "com.github.ornicar",
    name := "scalalib",
    version := "2.0",
    scalaVersion := "2.9.1",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % "6.0.4",
      "org.specs2" %% "specs2" % "1.12",
      "joda-time" % "joda-time" % "2.1",
      "org.joda" % "joda-convert" % "1.2"
    ),
    scalacOptions := Seq("-deprecation", "-unchecked"),
    publishTo := Some(Resolver.sftp(
      "iliaz",
      "scala.iliaz.com"
    ) as ("scala_iliaz_com", Path.userHome / ".ssh" / "id_rsa"))
  )
}
