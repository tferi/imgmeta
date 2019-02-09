package com.tothferenc.imgmeta.reporting

import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.Path
import java.nio.file.StandardOpenOption._

import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, ZipWith}
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString
import akka.{Done, NotUsed}

import scala.concurrent.{Future, Promise}

/**
  * Writes received bytes to the given path.
  * Existing content of file is truncated if it exists.
  * Outputs ByteStrings that have been written.
  */
class NioFileWriter(path: Path) extends GraphStageWithMaterializedValue[FlowShape[ByteString, ByteString], Future[Done]] {
  val in: Inlet[ByteString] = Inlet[ByteString](s"${this.getClass.getSimpleName}.in")
  val out: Outlet[ByteString] = Outlet[ByteString](s"${this.getClass.getSimpleName}.out")

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val p = Promise[Done]

    val logic: GraphStageLogic = new GraphStageLogic(shape) {

      private var fileChannel: AsynchronousFileChannel = _
      private var position: Long = 0L

      override def preStart(): Unit = {
        super.preStart()
        fileChannel = AsynchronousFileChannel.open(path, CREATE, TRUNCATE_EXISTING, WRITE)
      }

      override def postStop(): Unit = {
        fileChannel.close()
        super.postStop()
      }

      setHandler(in, new InHandler {

        private val bytesWritten = getAsyncCallback[ByteString] { bytes =>
          position += bytes.length
          push(out, bytes)
        }

        private val fail = getAsyncCallback[Throwable] { t =>
          p.tryFailure(t)
          failStage(t)
        }

        override def onPush(): Unit = {
          val elem = grab(in)
          if (elem.nonEmpty)
            fileChannel.write(elem.toByteBuffer, position, "", new CompletionHandler[Integer, String] {
              override def completed(result: Integer, attachment: String): Unit = bytesWritten.invoke(elem)

              override def failed(exc: Throwable, attachment: String): Unit = fail.invoke(exc)
            })
          else {
            push(out, elem)
          }
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


    }
    logic -> p.future
  }

  override def shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)
}

object NioFileWriter {
  def apply(path: Path): Flow[ByteString, ByteString, Future[Done]] = Flow.fromGraph(new NioFileWriter(path))

  def via[T](conversion: Flow[T, ByteString, NotUsed], path: Path): Flow[T, T, Future[Done]] = {
    val writerFlow = apply(path).drop(1L)
    Flow.fromGraph(GraphDSL.create(conversion, writerFlow)(Keep.right) { implicit b =>
      (convert, write) =>
        import GraphDSL.Implicits._

        val broadcast = b.add(Broadcast[T](2))

        val wait = b.add(ZipWith[T, ByteString, T](Keep.left))

        broadcast ~> wait.in0
        broadcast ~> convert ~> write ~> wait.in1

        FlowShape(broadcast.in, wait.out)
    })
  }
}