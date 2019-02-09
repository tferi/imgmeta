package com.tothferenc.imgmeta.akkastream

import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import com.tothferenc.imgmeta.extraction.AsyncImageProcessor
import com.tothferenc.imgmeta.model.{StreamIn, StreamOut}
import com.tothferenc.imgmeta.util.DirectEC

import scala.collection.immutable.Iterable
import scala.concurrent.Future

final case class PipelineHandle( doneF: Future[Done])

object ReactivePipeline {

  private def process(imageProcessor: AsyncImageProcessor, input: StreamIn): Future[StreamOut] = {
    input match {
      case StreamIn.AlbumAnnouncement(album) =>
        Future.successful(StreamOut.AlbumAnnouncement(album))
      case StreamIn.Elem(image) =>
        imageProcessor.process(image).map(StreamOut.Elem)(DirectEC)
    }
  }


  def run(dataSource: Source[StreamIn, NotUsed],
          reporters: Iterable[Flow[StreamOut, StreamOut, Future[Done]]],
          imageProcessor: AsyncImageProcessor,
          processorParallelism: Int,
          printStreamReporter: Flow[StreamOut, StreamOut, Future[Done]])(implicit m: Materializer) = {
    val graph = assemblePipeline(dataSource, reporters, imageProcessor, processorParallelism, printStreamReporter)
    PipelineHandle( RunnableGraph.fromGraph(graph).run())

  }

  private def assemblePipeline(dataSource: Source[StreamIn, NotUsed], reporters: Iterable[Flow[StreamOut, StreamOut, Future[Done]]], imageProcessor: AsyncImageProcessor, processorParallelism: Int, printStreamReporter: Flow[StreamOut, StreamOut, Future[Done]]) = {
    val reporterFlow = reporters.reduceOption(_ via _.async).getOrElse(Flow[StreamOut])

    val graph = dataSource.async
      .mapAsync[StreamOut](processorParallelism)(in => process(imageProcessor, in))
      .via(reporterFlow)
      .via(printStreamReporter)
      .toMat(Sink.ignore)(Keep.right)
    graph
  }
}
