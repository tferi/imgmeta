package com.tothferenc.imgmeta.datasource

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.tothferenc.imgmeta.model.StreamIn

trait DataSource {

  def getImageFiles(): Source[StreamIn, NotUsed]
}
