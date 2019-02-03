package com.tothferenc.imgmeta.extraction

import java.io.InputStream

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata

import scala.concurrent.{ExecutionContext, Future}

class AsyncMetaExtractor(implicit ec: ExecutionContext) {

  def extractMeta(inputStream: InputStream): Future[Metadata] = Future { ImageMetadataReader.readMetadata(inputStream) }
}
