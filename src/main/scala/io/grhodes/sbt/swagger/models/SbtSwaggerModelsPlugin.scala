package io.grhodes.sbt.swagger.models

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object SbtSwaggerModelsPlugin extends AutoPlugin {

  override def trigger: PluginTrigger = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    val swaggerGenerateModels: TaskKey[Seq[File]] = taskKey[Seq[java.io.File]]("Generate models for swagger APIs")
    val swaggerSourceDirectory: SettingKey[File] = settingKey[File]("Directory containing input swagger files")
    val swaggerOutputDirectory: SettingKey[File] = settingKey[File]("Directory into which the source code should be generated")
    val swaggerOutputPackage: SettingKey[String] = settingKey[String]("Package into which the source code should be generated")
    val swaggerSpecFilename: SettingKey[Option[String]] = settingKey[Option[String]]("Optionally specify the service swagger spec file (all other swagger files are client specs)")
    val swaggerEnumVendorExtensionName: SettingKey[Option[String]] = settingKey[Option[String]]("Optionally specify a vendor extension to be handled as an 'enum'")
    val swaggerDisableTypesafeIds: SettingKey[Boolean] = settingKey[Boolean]("Request code generation to be done without typesafe Id encapsulation")
    val swaggerTaggedAttributes: SettingKey[Seq[String]] = settingKey[Seq[String]]("Optionally tagged attributes")
    val swaggerGenerator: SettingKey[String] = settingKey[String]("The swagger-codegen generator to use")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    swaggerSourceDirectory := (resourceDirectory in Compile).value,
    swaggerOutputDirectory := (sourceManaged in Compile).value / "swagger",
    swaggerOutputPackage := "swagger",
    swaggerSpecFilename := Some("api.yaml"),
    swaggerEnumVendorExtensionName := None,
    swaggerDisableTypesafeIds := false,
    swaggerTaggedAttributes := Seq(),
    swaggerGenerator := "simple-scala",
    swaggerGenerateModels := ModelGenerator(
      streams = streams.value,
      sourceDir = (swaggerSourceDirectory in swaggerGenerateModels).value,
      targetDir = (swaggerOutputDirectory in swaggerGenerateModels).value,
      specFile = (swaggerSpecFilename in swaggerGenerateModels).value,
      basePkg = (swaggerOutputPackage in swaggerGenerateModels).value,
      disableTypesafeIds = swaggerDisableTypesafeIds.value,
      enumVendorExtensionName = swaggerEnumVendorExtensionName.value,
      taggedAttributes = swaggerTaggedAttributes.value,
      generator = swaggerGenerator.value
    ),
    watchSources ++= ((swaggerSourceDirectory in swaggerGenerateModels).value ** ModelGenerator.fileFilter).get,
    managedSourceDirectories in Compile += (swaggerOutputDirectory in swaggerGenerateModels).value,
    sourceGenerators in Compile += swaggerGenerateModels.taskValue
  )

}
