package com.tothferenc.imgmeta

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import org.slf4j.LoggerFactory

class Slf4jLog[T](name: String) extends GraphStage[FlowShape[T, T]] {
  val in: Inlet[T] = Inlet[T](s"${this.getClass.getSimpleName}.in")
  val out: Outlet[T] = Outlet[T](s"${this.getClass.getSimpleName}.out")
  private val logger = LoggerFactory.getLogger(name)


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onUpstreamFinish(): Unit = super.onUpstreamFinish()

      override def onUpstreamFailure(ex: Throwable): Unit = super.onUpstreamFailure(ex)

      override def onPush(): Unit = {
        val elem = grab(in)
        logger.trace("push {}", elem)
        push(out, elem)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        logger.trace("pull")
        pull(in)
      }

      override def onDownstreamFinish(): Unit = super.onDownstreamFinish()
    })
  }

  override def shape: FlowShape[T, T] = FlowShape(in, out)
}


object Slf4jLog {
  def apply[T](name: String): Flow[T, T, NotUsed] = Flow.fromGraph(new Slf4jLog[T](name))
}