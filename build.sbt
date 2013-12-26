name := "async-dbx-client"

organization := "eu.mrico"

version := "0.1.0"

scalaVersion := "2.10.3"


// == Compiler configuration ==

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")


// == API Documentation ==

autoAPIMappings := true

scalacOptions in (Compile, doc) ++= Opts.doc.title("asyncdbx")

scalacOptions in (Compile, doc) ++= Seq("-doc-root-content", "src/main/resources/rootdoc.txt")


// == Dependencies ==

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.3"

libraryDependencies += "io.spray" % "spray-can" % "1.2.0"

libraryDependencies += "io.spray" % "spray-client" % "1.2.0"

libraryDependencies += "io.spray" %%  "spray-json" % "1.2.5"


// == Test Dependencies ==

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.2.3" % "test"
