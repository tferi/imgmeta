package com.tothferenc.imgmeta.extraction

import com.tothferenc.imgmeta.model.{Image, ProcessedImage}
import com.tothferenc.imgmeta.util.DirectEC

import scala.concurrent.Future
import scala.util.Success

class AsyncImageProcessor(metaExtractor: AsyncMetaExtractor) {

  def process(imageFile: Image): Future[ProcessedImage] =
    metaExtractor.extractMeta(imageFile.getContents())
      .transform(t => Success(ProcessedImage(imageFile.dataSource, imageFile.album, imageFile.name, t)))(DirectEC)
}
