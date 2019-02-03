package com.tothferenc.imgmeta.akkastream

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters.fromJavaStream
import com.tothferenc.imgmeta.datasource.DataSource
import com.tothferenc.imgmeta.model.StreamIn

object AkkaDataSource {
  def apply(dataSource: DataSource): Source[StreamIn, NotUsed] = fromJavaStream(() =>dataSource.getImageFiles())
}
