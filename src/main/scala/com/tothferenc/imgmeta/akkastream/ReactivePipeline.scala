package com.tothferenc.imgmeta.akkastream

import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream._
import akka.{Done, NotUsed}
import com.tothferenc.imgmeta.extraction.AsyncImageProcessor
import com.tothferenc.imgmeta.model.{Image, ProcessedImage, StreamIn, StreamOut}
import com.tothferenc.imgmeta.util.DirectEC

import scala.collection.immutable.Iterable
import scala.concurrent.{ExecutionContext, Future}

class PipelineHandle(val killSwitch: KillSwitch, val reporterFutures: Seq[Future[Done]]) {

  val allReportersComplete: Future[Seq[Done]] = {
    implicit val directEC: ExecutionContext = DirectEC
    Future.sequence(reporterFutures)
  }
}

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
          reporters: Seq[Sink[StreamOut, Future[Done]]],
          imageProcessor: AsyncImageProcessor,
          processorParallelism: Int)(implicit m: Materializer) = {
    val combinedSource = dataSources.reduceOption(_ concat _).getOrElse(Source.empty)
    val (futures, sinks) = reporters.map(_.preMaterialize()).unzip

    val src = combinedSource
      .mapAsync[StreamOut](processorParallelism)(in => process(imageProcessor, in)).viaMat(KillSwitches.single)(Keep.right)

    val graph = GraphDSL.create(
      src) { implicit b =>
      resultSource =>
        import GraphDSL.Implicits._

        val broadcast = b.add(Broadcast[StreamOut](reporters.size))

        resultSource ~> broadcast
        sinks.foreach(broadcast ~> _)

        ClosedShape
    }

    val killswitch = RunnableGraph.fromGraph(graph).run()
    new PipelineHandle(killswitch, futures)

  }
}
