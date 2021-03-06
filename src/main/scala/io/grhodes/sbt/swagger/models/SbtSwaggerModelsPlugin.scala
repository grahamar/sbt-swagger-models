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
    val swaggerOutputPackage: SettingKey[Option[String]] = settingKey[Option[String]]("Package into which the source code should be generated")
    val swaggerApiPackage: SettingKey[Option[String]] = settingKey[Option[String]]("Package into which the api source code should be generated")
    val swaggerModelPackage: SettingKey[Option[String]] = settingKey[Option[String]]("Package into which the model source code should be generated")
    val swaggerSpecFilename: SettingKey[Option[String]] = settingKey[Option[String]]("Optionally specify the service swagger spec file (all other swagger files are client specs)")
    val swaggerEnumVendorExtensionName: SettingKey[Option[String]] = settingKey[Option[String]]("Optionally specify a vendor extension to be handled as an 'enum'")
    val swaggerDisableTypesafeIds: SettingKey[Boolean] = settingKey[Boolean]("Request code generation to be done without typesafe Id encapsulation")
    val swaggerTaggedAttributes: SettingKey[Seq[String]] = settingKey[Seq[String]]("Optionally tagged attributes")
    val swaggerGenerator: SettingKey[String] = settingKey[String]("The swagger-codegen generator to use")
    val swaggerGeneratorVerbose: SettingKey[Boolean] = settingKey[Boolean]("Turn on verbose for swagger-codegen generator")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    swaggerSourceDirectory := (resourceDirectory in Compile).value,
    swaggerOutputPackage := None,
    swaggerModelPackage := None,
    swaggerApiPackage := None,
    swaggerSpecFilename := Some("api.yaml"),
    swaggerEnumVendorExtensionName := None,
    swaggerDisableTypesafeIds := false,
    swaggerTaggedAttributes := Seq(),
    swaggerGenerator := "simple-scala",
    swaggerGeneratorVerbose := false,
    swaggerGenerateModels := ModelGenerator(
      streams = streams.value,
      srcManagedDir = (sourceManaged in Compile).value / "swagger",
      srcDir = (swaggerSourceDirectory in swaggerGenerateModels).value,
      baseDir = (baseDirectory in Compile).value,
      specFile = (swaggerSpecFilename in swaggerGenerateModels).value,
      basePkg = (swaggerOutputPackage in swaggerGenerateModels).value,
      modelPkg = (swaggerModelPackage in swaggerGenerateModels).value,
      apiPkg = (swaggerApiPackage in swaggerGenerateModels).value,
      disableTypesafeIds = swaggerDisableTypesafeIds.value,
      enumVendorExtensionName = swaggerEnumVendorExtensionName.value,
      taggedAttributes = swaggerTaggedAttributes.value,
      generator = swaggerGenerator.value,
      verbose = swaggerGeneratorVerbose.value
    ),
    sourceGenerators in Compile += swaggerGenerateModels.taskValue
  )

}
