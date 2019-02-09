package com.tothferenc.imgmeta.reporting

import java.io.PrintStream

import akka.Done
import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import com.tothferenc.imgmeta.model.{Album, StreamOut}

import scala.concurrent.{Future, Promise}

class PrintStreamReporter(printStream: PrintStream, dotsPerAlbum: Int) extends GraphStageWithMaterializedValue[FlowShape[StreamOut, StreamOut], Future[Done]] {

  val in: Inlet[StreamOut] = Inlet[StreamOut](s"${this.getClass.getSimpleName}r.in")
  val out: Outlet[StreamOut] = Outlet[StreamOut](s"${this.getClass.getSimpleName}.out")

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
              printStream.println(s"Processing album '${a.name}' from data source '${a.dataSource}', containing ${a.elemCount.fold("an unknown number of")(_.toString)} element(s)")
            case StreamOut.Elem(image) =>
              if (image.metadata.isFailure) errors = errors + 1
              album.fold {
                failStage(new IllegalStateException(s"Received element $image before album announcement"))
              } { a =>
                if (a.name != image.album) failStage(new IllegalStateException("Received image from different album."))
                a.elemCount.filter(_ != 0).foreach { expected =>
                  received = received + 1
                  val dotsForProgress = (received * dotsPerAlbum) / expected
                  val dotsToPrint = dotsForProgress - dotsPrinted

                  if (dotsToPrint > 0) {
                    val dots = "." * dotsToPrint
                    printStream.print(dots)
                    if (received == expected)
                      printStream.println("All announced elements processed.")
                  }
                  dotsPrinted = dotsForProgress

                }
              }
          }
          push(out, elem)
        }

        override def onUpstreamFinish(): Unit = {
          p.trySuccess(Done)
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          p.tryFailure(ex)
          super.onUpstreamFailure(ex)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)

        override def onDownstreamFinish(): Unit = {
          p.trySuccess(Done)
          super.onDownstreamFinish()
        }
      })

      private def printErrorsIfAny = {
        if (errors != 0) {
          printStream.println(s"Encountered $errors errors during processing previous album.")
          errors = 0
        }
      }
    }

    (logic, p.future)
  }

  override def shape: FlowShape[StreamOut, StreamOut] = FlowShape(in, out)
}

object PrintStreamReporter {
  def apply(ps: PrintStream, dotsPerAlbum: Int): Flow[StreamOut, StreamOut, Future[Done]] =
    Flow.fromGraph(new PrintStreamReporter(ps, dotsPerAlbum))
}
