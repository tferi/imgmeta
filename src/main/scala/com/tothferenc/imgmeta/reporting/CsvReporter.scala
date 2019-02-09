package com.tothferenc.imgmeta.reporting

import java.nio.file.Path

import akka.stream.alpakka.csv.impl.ToCsv
import akka.stream.scaladsl.{Flow, Source}
import akka.{Done, NotUsed}
import com.tothferenc.imgmeta.model.{ProcessedImage, StreamOut}

import scala.collection.JavaConverters.iterableAsScalaIterable
import scala.collection.{breakOut, mutable}
import scala.concurrent.Future
import scala.util.Success

class CsvReporter(outputFile: Path, tags: List[Int]) {


  private def header: List[String] = "dataSource" :: "album" :: "name" :: tags.map(i => s"tag_${i.toHexString}")

  def writerFlow: Flow[StreamOut, StreamOut, Future[Done]] = {
    NioFileWriter.via[StreamOut](toStringValues.map(ToCsv.format), outputFile)
  }

  private def toStringValues: Flow[StreamOut, Option[List[String]], NotUsed] = Flow[StreamOut].collect {
    case StreamOut.Elem(ProcessedImage(ds, a, n, Success(meta))) =>
      val found = new mutable.TreeMap[Integer, String]
      iterableAsScalaIterable(meta.getDirectories).foreach { dir =>
        tags.foreach { i =>
          if (dir.hasTagName(i)) found.+=((i, dir.getString(i)))
        }
      }
      val tagValues: List[String] = tags.map(i => found.getOrElse(i, ""))(breakOut)
      Some(ds :: a :: n :: tagValues)
    case _ => None
  }.prepend(Source.single(Some(header)))

}
