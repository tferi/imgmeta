name := "imgmeta"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.20",
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "1.0-M2",
  "com.github.scopt" % "scopt_2.11" % "4.0.0-RC2",
  "com.drewnoakes" % "metadata-extractor" % "2.11.0",

  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.20" % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

fork := true