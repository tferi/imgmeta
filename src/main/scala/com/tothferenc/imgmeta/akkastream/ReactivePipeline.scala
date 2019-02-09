package com.tothferenc.imgmeta.akkastream

import akka.stream._
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import com.tothferenc.imgmeta.Slf4jLog
import com.tothferenc.imgmeta.extraction.AsyncImageProcessor
import com.tothferenc.imgmeta.model.{StreamIn, StreamOut}
import com.tothferenc.imgmeta.util.DirectEC

import scala.collection.immutable.Iterable
import scala.concurrent.Future

final case class PipelineHandle(killSwitch: KillSwitch, doneF: Future[Done])

object ReactivePipeline {

  private def process(imageProcessor: AsyncImageProcessor, input: StreamIn): Future[StreamOut] = {
    input match {
      case StreamIn.AlbumAnnouncement(album) =>
        Future.successful(StreamOut.AlbumAnnouncement(album))
      case StreamIn.Elem(image) =>
        imageProcessor.process(image).map(StreamOut.Elem)(DirectEC)
    }
  }


  def run(dataSources: Iterable[Source[StreamIn, NotUsed]],
          reporters: Iterable[Flow[StreamOut, StreamOut, Future[Done]]],
          imageProcessor: AsyncImageProcessor,
          processorParallelism: Int)(implicit m: Materializer) = {
    val combinedSource = dataSources.reduceOption(_ concat _).getOrElse(Source.empty)
    val reporterFlow = reporters.reduceOption(_ via _).getOrElse(Flow[StreamOut])

    val graph = combinedSource
      .mapAsync[StreamOut](processorParallelism)(in => process(imageProcessor, in))
      .via(Slf4jLog("source"))
      .viaMat(KillSwitches.single)(Keep.right)
      .via(Slf4jLog("killswitch"))
      .via(reporterFlow)
      .toMat(Sink.ignore)(Keep.both)

    val (killswitch, doneF) = RunnableGraph.fromGraph(graph).run()
    PipelineHandle(killswitch, doneF)

  }
}
