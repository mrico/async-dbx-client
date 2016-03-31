name := "async-dbx-client"

organization := "eu.mrico"

version := "0.2.0"

scalaVersion := "2.11.8"

// == Compiler configuration ==

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")


// === Test configuration ==

parallelExecution in Test := false


// == API Documentation ==

autoAPIMappings := true

scalacOptions in (Compile, doc) ++= Opts.doc.title("asyncdbx")

scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", "src/main/resources/rootdoc.txt")


// == Dependencies ==

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.13"

libraryDependencies += "io.spray" %% "spray-can" % "1.3.1"

libraryDependencies += "io.spray" %% "spray-client" % "1.3.1"

libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"

libraryDependencies += "org.scala-lang.modules"  %% "scala-xml" % "1.0.0"

// == Test Dependencies ==

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test"
