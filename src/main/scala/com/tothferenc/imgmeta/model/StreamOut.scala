package com.tothferenc.imgmeta.model

sealed abstract class StreamOut extends Product with Serializable

object StreamOut {
  case class AlbumAnnouncement(album: Album) extends StreamOut
  case class Elem(image: ProcessedImage) extends StreamOut
}