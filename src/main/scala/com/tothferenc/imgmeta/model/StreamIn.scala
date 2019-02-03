package com.tothferenc.imgmeta.model

sealed abstract class StreamIn extends Product with Serializable
object StreamIn {
  case class AlbumAnnouncement(album: Album) extends StreamIn
  case class Elem(image: Image) extends StreamIn
}



