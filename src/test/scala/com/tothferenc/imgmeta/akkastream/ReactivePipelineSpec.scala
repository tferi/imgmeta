package com.tothferenc.imgmeta.akkastream

import java.nio.file.{Files, Path}

import akka.stream.scaladsl.{FileIO, Sink}
import com.tothferenc.imgmeta.WellKnownTags.focalLength
import com.tothferenc.imgmeta.datasource.DirectoryDataSource
import com.tothferenc.imgmeta.extraction.{AsyncImageProcessor, AsyncMetaExtractor}
import com.tothferenc.imgmeta.model.StreamIn
import com.tothferenc.imgmeta.reporting.{CsvReporter, PrintStreamReporter}
import org.scalatest.AsyncWordSpec

class ReactivePipelineSpec extends AsyncWordSpec with AkkaBeforeAndAfterAll {

  val testResources = Path.of("src").resolve("test").resolve("resources")

  val ds1 = testResources.resolve("ds1")
  val ds2 = testResources.resolve("ds2")

  val dataSources = List(ds1, ds2).map { p =>
    DirectoryDataSource(p).getImageFiles()
  }

  val out1 = Files.createTempFile(this.getClass.getSimpleName, System.currentTimeMillis().toString)
  val consoleReporter = PrintStreamReporter(System.out, 10)

  val reporters = consoleReporter :: List(out1).map(new CsvReporter(_, List(focalLength)).getSink)

  val proc = new AsyncImageProcessor(new AsyncMetaExtractor())

  "Reactive Pipeline" should {
    "connect all inputs and outputs" in {
      val handle = ReactivePipeline.run(dataSources, reporters, proc, 4)

      for {
        f <- handle.allReportersComplete
      } yield {
        succeed
      }
    }
  }

}
