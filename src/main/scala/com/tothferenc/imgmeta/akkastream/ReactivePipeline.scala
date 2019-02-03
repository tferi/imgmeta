package com.tothferenc.imgmeta.akkastream

import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.stream._
import akka.{Done, NotUsed}
import com.tothferenc.imgmeta.extraction.AsyncImageProcessor
import com.tothferenc.imgmeta.model.{Image, ProcessedImage}
import com.tothferenc.imgmeta.util.DirectEC

import scala.collection.immutable.Iterable
import scala.concurrent.Future

class PipelineHandle(val killSwitch: KillSwitch, val reporterFutures: Seq[Future[Done]]) {

  val allReportersComplete: Future[Seq[Done]] = {
    implicit val directEC = DirectEC
    Future.sequence(reporterFutures)
  }
}

object ReactivePipeline {


  def run(dataSources: Iterable[Source[Image, NotUsed]],
            reporters: Seq[Sink[ProcessedImage, Future[Done]]],
            imageProcessor: AsyncImageProcessor,
            processorParallelism: Int)(implicit m: Materializer) = {
    val combinedSource = dataSources.reduceOption(_ concat _).getOrElse(Source.empty)
    val (futures, sinks) = reporters.map(_.preMaterialize()).unzip

    val src = combinedSource
      .mapAsync(processorParallelism)(imageProcessor.process).viaMat(KillSwitches.single)(Keep.right)

    val graph = GraphDSL.create(
      src) { implicit b => resultSource =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[ProcessedImage](reporters.size))

      resultSource ~> broadcast
      sinks.foreach(broadcast  ~> _)

      ClosedShape
    }

    val killswitch = RunnableGraph.fromGraph(graph).run()
    new PipelineHandle(killswitch, futures)

  }
}
