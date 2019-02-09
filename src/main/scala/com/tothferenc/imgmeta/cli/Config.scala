package com.tothferenc.imgmeta.cli

import java.nio.file.Path

import scopt.{OParser, Read}

final case class Config(dataSources: Vector[Path], outputs: Vector[Path], parallelism: Int, cycle: Boolean)

object Config {

  private val readPath: Read[Path] = Read.reads(Path.of(_))

  private def parser = {
    val b = OParser.builder[Config]
    import b._

    OParser.sequence(
      opt[Path]('i', "in")(readPath)
        .required()
        .action((p, c) => c.copy(dataSources = c.dataSources :+ p))
        .text("Path to an input data source (directory containing image files)"),
      opt[Path]('o', "out")(readPath)
        .required()
        .action((p, c) => c.copy(outputs = c.outputs :+ p))
        .text("Path to an output CSV"),
      opt[Int]('p', "parallelism")
        .action((p, c) => c.copy(parallelism = p))
        .optional()
        .text("Number of parallel image processing pipelines. Defaults to the number of virtual processors available to the JVM."),
      opt[Unit]('c', "cycle")
        .action((_, c) => c.copy(cycle = true))
        .optional()
        .text("Debugging option. Repeat running on the datasources until terminated."),
      b.help("help")
    )
  }

  def parse(args: Array[String]) = OParser.parse(parser, args, Config(Vector.empty, Vector.empty, Runtime.getRuntime.availableProcessors(), false))
}
