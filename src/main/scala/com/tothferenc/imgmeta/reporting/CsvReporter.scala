package com.tothferenc.imgmeta.reporting

import java.nio.file.Path

import akka.Done
import akka.stream.alpakka.csv.scaladsl.CsvFormatting
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import com.tothferenc.imgmeta.model.{ProcessedImage, StreamOut}
import com.tothferenc.imgmeta.util.DirectEC

import scala.collection.JavaConverters.iterableAsScalaIterable
import scala.collection.{breakOut, mutable}
import scala.concurrent.Future
import scala.util.{Failure, Success}

class CsvReporter(outputFilePath: Path, tags: List[Int]) {

  private def header: List[String] = "dataSource" :: "album" :: "name" :: tags.map(i => s"tag_${i.toHexString}")

  def getSink: Sink[StreamOut, Future[Done]] = toStringValues.via(CsvFormatting.format[List[String]]()).toMat(byteSink)(Keep.right)

  private def toStringValues = Flow[StreamOut].collect {
    case StreamOut.Elem(ProcessedImage(ds, a, n, Success(meta))) =>
      val found = new mutable.TreeMap[Integer, String]
      iterableAsScalaIterable(meta.getDirectories).foreach { dir =>
        tags.foreach { i =>
          if (dir.hasTagName(i)) found.+=((i, dir.getString(i)))
        }
      }
      val tagValues: List[String] = tags.map(i => found.getOrElse(i, ""))(breakOut)
      ds :: a :: n :: tagValues
  }.prepend(Source.single(header))

  private def byteSink: Sink[ByteString, Future[Done]] = FileIO.toPath(outputFilePath).mapMaterializedValue(_.transform {
    case Success(res) => res.status
    case Failure(t) => Failure(t)
  }(DirectEC))
}
