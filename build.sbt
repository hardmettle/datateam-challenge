name := "datateam-challenge"

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies += "com.nrinaudo" %% "kantan.csv-generic" % "0.1.18"
//FIXME:Please do a publish local to resolve this dependency
//libraryDependencies += "net.debasishg" %% "redisreact" % "0.9"
libraryDependencies += "net.debasishg" %% "redisclient" % "3.3"
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"