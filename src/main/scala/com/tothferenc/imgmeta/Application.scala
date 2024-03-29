package com.tothferenc.imgmeta

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
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


  def main(args: Array[String]): Unit = {
    Config.parse(args).fold {
      illegalArg("Couldn't parse args")
    } { c =>
      validateConfig(c)
      try {
        logger.info("Running with config {}", c)
        val executor = Executors.newFixedThreadPool(c.parallelism, (r: Runnable) => {
          val t = new Thread(r)
          t.setDaemon(true)
          t
        })
        implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executor)
        val dataSources = (if (c.cycle) Source.cycle(() => c.dataSources.iterator) else Source(c.dataSources))
          .flatMapConcat(DirectoryDataSource(_).getImageFiles())
        val outputs = c.outputs.map(new CsvReporter(_, List(WellKnownTags.focalLength)).writerFlow)
        val imageProcessor = new AsyncImageProcessor(new AsyncMetaExtractor())
        ReactivePipeline.run(
          dataSources,
          outputs,
          imageProcessor,
          c.parallelism,
          PrintStreamReporter(System.out, 50)).doneF.onComplete {
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

  private def validateConfig(c: Config): Unit = {
    if (c.outputs.isEmpty) illegalArg("At least 1 output must be specified")
    if (c.dataSources.isEmpty) illegalArg("At least 1 input must be specified")
    c.dataSources.find(p => !p.toFile.exists())
      .foreach(p => illegalArg(s"Path does not exist: $p"))
    c.outputs.find(p => !p.getParent.toFile.exists())
      .foreach(p => illegalArg(s"Can't create output at $p as parent folder does not exist."))
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
