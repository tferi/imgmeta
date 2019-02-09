package com.tothferenc.imgmeta

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.tothferenc.imgmeta.akkastream.ReactivePipeline
import com.tothferenc.imgmeta.cli.Config
import com.tothferenc.imgmeta.datasource.DirectoryDataSource
import com.tothferenc.imgmeta.extraction.{AsyncImageProcessor, AsyncMetaExtractor}
import com.tothferenc.imgmeta.reporting.{CsvReporter, PrintStreamReporter}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object Application {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private implicit lazy val system = ActorSystem()
  private implicit lazy val mat = ActorMaterializer()


  private val processors: Int = Runtime.getRuntime.availableProcessors()
  private lazy val executor = Executors.newFixedThreadPool(processors, (r: Runnable) => {
    val t = new Thread(r)
    t.setDaemon(true)
    t
  })

  def main(args: Array[String]): Unit = {
    Config.parse(args).fold {
      illegalArg("Couldn't parse args")
    } { c =>
      if (c.outputs.isEmpty) illegalArg("At least 1 output must be specified")
      if (c.dataSources.isEmpty) illegalArg("At least 1 input must be specified")
      try {
        logger.info("Running with config {}", c)
        implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)
        val dataSources = c.dataSources.map(DirectoryDataSource(_).getImageFiles())
        val outputs = c.outputs.map(new CsvReporter(_, List(WellKnownTags.focalLength)).writerFlow) :+ PrintStreamReporter(System.out, 50)
        val imageProcessor = new AsyncImageProcessor(new AsyncMetaExtractor())
        ReactivePipeline.run(
          dataSources,
          outputs,
          imageProcessor,
          processors).doneF.onComplete {
          case Success(s) => terminate()
          case Failure(t) => fail(t)
        }
      } finally {
        sys.addShutdownHook {
          terminate()
        }
      }
    }
  }

  private def terminate() = {
    if (!mat.isShutdown) {
      logger.info("Executing graceful shutdown.")
      mat.shutdown()
      Await.result(system.terminate(), 5.seconds)
    }
  }

  private def illegalArg(s: String): Unit = {
    fail(new IllegalArgumentException(s))
  }

  private def fail(t: Throwable): Unit = {
    logger.error("Execution failed", t)
    System.exit(-1)
  }


}
