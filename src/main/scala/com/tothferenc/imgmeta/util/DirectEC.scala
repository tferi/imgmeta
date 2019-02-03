package com.tothferenc.imgmeta.util

import scala.concurrent.ExecutionContext

object DirectEC extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()

  override def reportFailure(cause: Throwable): Unit = throw cause
}
