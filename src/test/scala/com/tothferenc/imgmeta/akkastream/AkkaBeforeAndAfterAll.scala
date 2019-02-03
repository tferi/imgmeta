package com.tothferenc.imgmeta.akkastream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait AkkaBeforeAndAfterAll extends BeforeAndAfterAll { self: Suite =>

  protected implicit var sys: ActorSystem = _
  protected implicit var mat: ActorMaterializer = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    sys = ActorSystem()
    mat = ActorMaterializer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    mat.shutdown()
    Await.result(sys.terminate(), 5.seconds)
  }
}
