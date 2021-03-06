package io.grhodes.sbt.swagger.models

import java.util.ServiceLoader

import io.swagger.codegen.v3.{ClientOptInput, CodegenConfig, DefaultGenerator}
import io.swagger.codegen.v3.config.CodegenConfigurator
import org.apache.commons.io.FilenameUtils
import sbt._
import sbt.Keys.TaskStreams

import scala.annotation.tailrec

object ModelGenerator {

  val fileFilter: FileFilter = "*.yml" || "*.yaml" || "*.json"

  def apply(streams: TaskStreams,
            srcManagedDir: File,
            srcDir: File,
            baseDir: File,
            specFile: Option[String],
            basePkg: Option[String],
            modelPkg: Option[String],
            apiPkg: Option[String],
            disableTypesafeIds: Boolean,
            enumVendorExtensionName: Option[String],
            taggedAttributes: Seq[String],
            generator: String,
            verbose: Boolean): scala.Seq[java.io.File] = {

    streams.log.info(s"Source: $srcDir")

    val serviceSpec = specFile.map(srcDir / _)
    val cache = streams.cacheDirectory

    streams.log.info(s"Spec: $serviceSpec")

    val cachedCompile = FileFunction.cached(
      file(cache.getAbsolutePath) / "swagger",
      inStyle = FilesInfo.lastModified,
      outStyle = FilesInfo.exists
    )(compile(streams.log, srcManagedDir, baseDir, serviceSpec, basePkg, modelPkg, apiPkg, generator, verbose))

    cachedCompile((srcDir ** fileFilter).get.toSet).toSeq
  }

  /**
    * Resolves a swagger-codegen config instance by its generator name. We need
    * this because the underlying loading mechanism relies on
    * `ServiceLoader.load(java.lang.Class)`, which assumes the service
    * definition file can be loaded from the current thread's context
    * class loader. For SBT plugins, this isn't the case, and so we explicitly
    * get the loader of the `CodegenConfig` class and then attempt to load the
    * service loader directly. Assuming we can load it, we then search all
    * instances for a config by the given name.
    */
  private def resolveConfigFromName(name: String): Option[CodegenConfig] = {
    val cls = classOf[CodegenConfig]
    val it = ServiceLoader.load(cls, cls.getClassLoader).iterator()

    @tailrec
    def loop(): Option[CodegenConfig] = {
      if (it.hasNext) {
        val config = it.next()
        if (config.getName == name) Some(config)
        else loop()
      } else None
    }

    loop()
  }

  private def compile(log: Logger,
                      srcManagedDir: File,
                      baseDir: File,
                      specFile: Option[File],
                      basePkg: Option[String],
                      modelPkg: Option[String],
                      apiPkg: Option[String],
                      generator: String,
                      verbose: Boolean)(in: Set[File]) = {
    in.foreach { swaggerFile =>
      log.debug(s"Swagger spec: $swaggerFile")
      log.debug(s"Is spec: ${specFile.exists(_.asFile == swaggerFile.asFile)}")
      if(specFile.isEmpty || specFile.exists(_.asFile == swaggerFile.asFile)) {
        log.info(s"[$generator] Generating source files from Swagger spec: $swaggerFile")
        val generatorName = resolveConfigFromName(generator).getOrElse(sys.error(s"Failed to locate a generator by name $generator!"))
        runCodegen(swaggerFile.toURI.toString, srcManagedDir, baseDir, generatorName.getClass.getName, basePkg, modelPkg, apiPkg, verbose)
      }
    }

    (srcManagedDir ** "*.scala").get.toSet
  }

  private def runCodegen(swaggerFile: String,
                         srcManagedDir: File,
                         baseDir: File,
                         generator: String,
                         basePkg: Option[String],
                         modelPkg: Option[String],
                         apiPkg: Option[String],
                         verbose: Boolean) = {
    val configurator = new CodegenConfigurator()
    configurator.setVerbose(verbose)

    /* The `inputSpec` within the configurator can be a stringified URI.
       However it seems if you pass it a file:/ URI, it fails because it tests
       for a prefix of "file://". Therefore if we're dealing with a file URI,
       we stringify it to conform to that expectation. */
    val specLocation = if (swaggerFile.toLowerCase.startsWith("file:")) {
      s"file:///${swaggerFile.substring(6)}"
    } else {
      swaggerFile
    }

    configurator.setLang(generator)
    configurator.setInputSpecURL(specLocation)
    configurator.setOutputDir(baseDir.toString)
    configurator.addAdditionalProperty("sourceManagedDir", srcManagedDir.toString)

    // determine the scala package of the generated code by the
    // filename of the OpenAPI specification
    val invokerPackage = basePkg.getOrElse(FilenameUtils.getBaseName(swaggerFile))
    val apiPackage = apiPkg.orElse(Option(invokerPackage).filter(_.nonEmpty).map(_ + ".api"))
    val modelPackage = modelPkg.orElse(Option(invokerPackage).filter(_.nonEmpty).map(_ + ".model"))

    configurator.setInvokerPackage(invokerPackage)
    configurator.setModelPackage(modelPackage.getOrElse("model"))
    configurator.setApiPackage(apiPackage.getOrElse("api"))

    val input: ClientOptInput = configurator.toClientOptInput


    // configurator.toClientOptInput() attempts to read the file and parse an
    // OpenAPI specification. If it fails for any reason, that reason is
    // swallowed and null is returned. For now, this is our best bet at
    // providing a slightly less hostile error message, but we should look
    // manually constructing ClientOptInput, so we can control how parsing
    // errors are reported.
    Option(input.getOpenAPI)
      .map(_ => new DefaultGenerator().opts(input).generate())
      .getOrElse(sys.error(s"Failed to load OpenAPI specification from $swaggerFile! Is it valid?"))
  }

}
