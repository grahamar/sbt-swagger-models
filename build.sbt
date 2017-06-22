name := "sbt-swagger-models"
organization := "io.grhodes.sbt"

version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim.replace("-dirty", "-SNAPSHOT")

scalaVersion := "2.10.6"

sbtPlugin := true

resolvers += Resolver.bintrayRepo("grahamar", "maven")

libraryDependencies ++= Seq(
  "io.swagger" % "swagger-codegen" % "2.2.2",
  "io.grhodes" %% "simple-scala-generator" % "0.1.1",
  "org.scalactic" %% "scalactic" % "3.0.1" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

scalacOptions ++= List("-unchecked")

publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayPackageLabels := Seq("sbt","plugin")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

initialCommands in console := """import io.grhodes.sbt.swagger.models._"""

// set up 'scripted; sbt plugin for testing sbt plugins
ScriptedPlugin.scriptedSettings
scriptedBufferLog := false
scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-XX:MaxPermSize=256M",
  s"-Dplugin.version=${version.value}"
)
