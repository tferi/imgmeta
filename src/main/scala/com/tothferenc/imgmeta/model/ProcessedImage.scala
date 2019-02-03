package com.tothferenc.imgmeta.model

import com.drew.metadata.Metadata

import scala.util.Try

final case class ProcessedImage(dataSource: String, album: String, name: String, metadata: Try[Metadata])
