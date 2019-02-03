package com.tothferenc.imgmeta.datasource

import com.tothferenc.imgmeta.model.StreamIn

trait DataSource {

  def getImageFiles(): java.util.stream.Stream[StreamIn]
}
