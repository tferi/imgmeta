package com.tothferenc.imgmeta.cli

import java.nio.file.Path

import scopt.{OParser, Read}

final case class Config(dataSources: Vector[Path], outputs: Vector[Path])
object Config {

  private val readPath: Read[Path] = Read.reads(Path.of(_))

  private def parser = {
    val b = OParser.builder[Config]
    import b._

    OParser.sequence(
      opt[Path]('i', "in")(readPath)
        .action((p, c) => c.copy(dataSources = c.dataSources :+ p))
        .text("Path to an input data source (directory containing image files)"),
      opt[Path]('o', "out")(readPath)
        .action((p, c) => c.copy(outputs = c.outputs :+ p))
        .text("Path to an output CSV"),
      b.help("help")
    )
  }

  def parse(args: Array[String]) = OParser.parse(parser, args, Config(Vector.empty, Vector.empty))
}
