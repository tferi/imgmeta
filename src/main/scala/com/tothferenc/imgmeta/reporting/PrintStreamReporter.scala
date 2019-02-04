package com.tothferenc.imgmeta.reporting

import java.io.PrintStream

import akka.Done
import akka.stream.scaladsl.Sink
import akka.stream.{Attributes, Inlet, SinkShape}
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import com.tothferenc.imgmeta.model.{Album, StreamOut}

import scala.concurrent.{Future, Promise}
import scala.util.Success

class PrintStreamReporter(out: PrintStream, dotsPerAlbum: Int) extends GraphStageWithMaterializedValue[SinkShape[StreamOut], Future[Done]]{

  val in: Inlet[StreamOut] = Inlet[StreamOut]("PrintStreamReporter.in")

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val p = Promise[Done]()
    val logic = new GraphStageLogic(shape) {

      var album = Option.empty[Album]
      var received = 0
      var dotsPrinted = 0
      var errors = 0

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          elem match {
            case StreamOut.AlbumAnnouncement(a) =>
              printErrorsIfAny
              album = Some(a)
              received = 0
              dotsPrinted = 0
              out.println(s"Processing album '${a.name}' from data source '${a.dataSource}', containing ${a.elemCount.fold("an unknown number of")(_.toString)} element(s)")
            case StreamOut.Elem(image) =>
              if (image.metadata.isFailure) errors = errors + 1
              album.fold {
                failStage(new IllegalStateException(s"Received element $image before album announcement"))
              } { a=>
                if (a.name != image.album) failStage( new IllegalStateException("Received image from different album."))
                a.elemCount.filter(_ != 0).foreach { expected =>
                  received = received + 1
                  val dotsForProgress = (received * dotsPerAlbum) / expected
                  val dotsToPrint = dotsForProgress - dotsPrinted

                  if (dotsToPrint > 0) {
                    val dots = "."*dotsToPrint
                    out.print(dots)
                    if (received == expected)
                      out.println("All announced elements processed.")
                  }
                  dotsPrinted = dotsForProgress

                }
              }
          }
          if (!isClosed(in)) pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          p.complete(Success(Done))
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          p.failure(ex)
          super.onUpstreamFailure(ex)
        }
      })

      private def printErrorsIfAny = {
        if (errors != 0) {
          out.println(s"Encountered $errors errors during processing previous album.")
          errors = 0
        }
      }

      override def preStart(): Unit = {
        super.preStart()
        pull(in)
      }
    }

    (logic, p.future)
  }

  override def shape: SinkShape[StreamOut] = SinkShape(in)
}

object PrintStreamReporter {
  def apply(ps: PrintStream, dotsPerAlbum: Int): Sink[StreamOut, Future[Done]] =
    Sink.fromGraph(new PrintStreamReporter(ps, dotsPerAlbum))
}
