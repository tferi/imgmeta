package com.tothferenc.imgmeta

import java.util.concurrent.{Executors, ThreadFactory}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

object Application {

  private implicit val system = ActorSystem()
  private implicit val mat = ActorMaterializer()


  private val executor = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors(), (r: Runnable) => {
    val t = new Thread(r)
    t.setDaemon(true)
    t
  })

  def main(args: Array[String]): Unit = {
    println("hello")
  }

  sys.addShutdownHook {
    println("executing graceful shurdown")
    mat.shutdown()
    Await.result(system.terminate(), 5.seconds)
  }
}
