package com.tothferenc.imgmeta

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration._

object Application {

  implicit lazy val system = ActorSystem()
  implicit lazy val mat = ActorMaterializer()

  def main(args: Array[String]): Unit = {

  }

  sys.addShutdownHook {
    mat.shutdown()
    Await.result(system.terminate(), 5.seconds)
  }
}
