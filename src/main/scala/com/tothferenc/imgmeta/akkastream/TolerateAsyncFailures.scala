package com.tothferenc.imgmeta.akkastream

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.tothferenc.imgmeta.util.DirectEC

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

// TODO maybe redundant
object TolerateAsyncFailures {

  def apply[I, O](parallelism: Int)(fun: I => Future[O]): Flow[I, Try[O], NotUsed] =
    Flow[I].mapAsync(parallelism) { in =>
      fun(in).transform {
        case s@Success(o) => Success(s)
        case f@Failure(t) => Success(f)
      }(DirectEC)
    }
}
