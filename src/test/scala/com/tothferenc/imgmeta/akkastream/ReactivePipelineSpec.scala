package com.tothferenc.imgmeta.akkastream

import java.nio.file.{Files, Path}

import akka.stream.scaladsl.FileIO
import com.tothferenc.imgmeta.WellKnownTags.focalLength
import com.tothferenc.imgmeta.datasource.DirectoryDataSource
import com.tothferenc.imgmeta.extraction.{AsyncImageProcessor, AsyncMetaExtractor}
import com.tothferenc.imgmeta.reporting.{CsvReporter, PrintStreamReporter}
import org.scalatest.{AsyncWordSpec, Matchers}

class ReactivePipelineSpec extends AsyncWordSpec with AkkaBeforeAndAfterAll with Matchers {

  val testResources = Path.of("src").resolve("test").resolve("resources")

  val ds1 = testResources.resolve("ds1")
  val ds2 = testResources.resolve("ds2")

  val dataSources = List(ds1, ds2).map { p =>
    DirectoryDataSource(p).getImageFiles()
  }

  val out1 = Files.createTempFile(this.getClass.getSimpleName, System.currentTimeMillis().toString)
  val out2 = Files.createTempFile(this.getClass.getSimpleName, System.currentTimeMillis().toString)
  val consoleReporter = PrintStreamReporter(System.out, 10)

  val reporters = Vector(out1, out2).map(new CsvReporter(_, List(focalLength)).writerFlow)

  val proc = new AsyncImageProcessor(new AsyncMetaExtractor())

  "Reactive Pipeline" should {
    "connect all inputs and outputs" in {
      val handle = ReactivePipeline.run(dataSources, reporters, proc, 4,consoleReporter)

      for {
        f <- handle.doneF
        out1Contents <- FileIO.fromPath(out1).runFold("")(_ + _.utf8String)
        out2Contents <- FileIO.fromPath(out1).runFold("")(_ + _.utf8String)
        expectedContents <-FileIO.fromPath(testResources.resolve("expected.csv")).runFold("")(_ + _.utf8String)
      } yield {
        out1Contents shouldEqual out2Contents
        out1Contents shouldEqual expectedContents
      }
    }
  }

}
